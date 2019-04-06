package io.kurumi.ntt.funcs;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.twitter.TAuth;
import io.kurumi.ntt.utils.T;
import java.io.File;
import twitter4j.JSONArray;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.JSONException;

public class TwitterDelete extends Fragment {

    public static TwitterDelete INSTANCE = new TwitterDelete();
    
    final String POINT_DELETE_LIKES = "d|l";

    @Override
    public boolean onNPM(UserData user,Msg msg) {

        msg.send((msg.doc() == null) + "").exec();
        
        if (msg.doc() == null) return false;

        msg.send(msg.doc().fileName()).exec();
        
        switch (msg.doc().fileName()) {

            case "like.js" : deleteLikes(user,msg);break;

            default : return false;

        }

        return true;

    }

    @Override
    public boolean onPPM(UserData user,Msg msg) {

        switch (user.point.getPoint()) {

            case POINT_DELETE_LIKES : comfirmDeleteLikes(user,msg);break;

            default : return false;

        }

        return true;

    }

    void deleteLikes(UserData user,Msg msg) {

        msg.send("check").exec();
        
        if (T.checkUserNonAuth(user,msg)) return;

        msg.send("输入 任意内容 来删除所有的推文喜欢 ","使用 /cancel 取消 注意 : 开始后不可撤销").exec();

        user.point = cdata(POINT_DELETE_LIKES);

        user.point.setIndex(msg.doc().fileId());

        user.savePoint();

    }

    void comfirmDeleteLikes(final UserData user,final Msg msg) {

        try {

            msg.sendTyping();

            File likejs = getFile(user.point.getIndex());

            String content = FileUtil.readUtf8String(likejs);

            msg.send("content length = " + content.length()).exec();
            
            content = StrUtil.subAfter(content,"=",false);
            
            msg.send("subed : " + content.length()).exec();

            final JSONArray array  = new JSONArray(content);

            msg.send("解析成功 : " + array.length() + "个喜欢 正在删除...").exec();

            final Twitter api = TAuth.get(user).createApi();

            new Thread("NTT Twitter Likes Delete Thread") {

                @Override
                public void run() {

                    for (int index = 0;index > array.length();index ++) {

                        try {
                            
                            api.destroyFavorite(array.getJSONObject(index).getJSONObject("like").getLong("tweetId"));
                            
                        } catch (TwitterException e) {
                            
                    e.printStackTrace();
                            
                        } catch (JSONException e) {
                            
                            e.printStackTrace();
                            
                        }

                    }

                    msg.send("喜欢删除完成 ~").exec();

                }

            }.start();

            user.point = null;
            
            user.savePoint();

        } catch (Exception err) {

            msg.send("解析失败..." + err).exec();

        }


    }

}
