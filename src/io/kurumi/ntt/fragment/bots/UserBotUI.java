package io.kurumi.ntt.fragment.bots;

import io.kurumi.ntt.funcs.abs.*;
import java.util.*;
import io.kurumi.ntt.db.*;
import io.kurumi.ntt.model.*;
import io.kurumi.ntt.model.request.*;
import io.kurumi.ntt.db.PointStore.*;
import io.kurumi.ntt.fragment.bots.UserBotUI.*;
import com.pengrad.telegrambot.*;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.model.*;
import com.mongodb.client.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import io.kurumi.ntt.fragment.*;


public class UserBotUI extends Function {
	
	public static void start() {
		
		for (UserBot bot : botData.collection.find()) {
		
		
			System.out.print(bot.type);
			
			System.out.print(" : ");
			
			System.out.println(bot.token);
			
			switch (bot.type) {
				
				case ChatForward : {
					
						ForwardBot fwd = new ForwardBot(bot.user,bot.token,bot.params.get(0));

						fwd.silentStart();
						
					} break;
				
				
			}
		
		}
		
	}
	
	public static class UserBot {

		public long id;

		public long user;

		public String userName;

		public String token;

		public String type;

		public List<String> params;

	}

	public static Data<UserBot> botData = new Data<UserBot>(UserBot.class);

	@Override
	public void functions(LinkedList<String> names) {

		names.add("newbot");
		names.add("setbot");
		names.add("delbot");

	}

	static final String POINT_NEW_BOT = "bot,new";
	static final String POINT_SET_BOT = "bot,set";
	static final String POINT_DEL_BOT = "bot,del";

	static final String ChatForward = "私聊BOT";

	@Override
	public void onFunction(final UserData user,Msg msg,String function,String[] params) {

		if (function.startsWith("new")) {

			msg.send("这只Bot需要干点什么呢 ~").keyboard(new Keyboard() {{

						newButtonLine(ChatForward);
						
						newButtonLine("/cancel");

					}}).exec();

			setPoint(user,POINT_NEW_BOT);

		} else if (function.startsWith("set")) {

			if (botData.countByField("user",user.id) == 0) {

				msg.send("乃还没有创建Bot呢... 试试 /newbot ~").exec();

				return;

			}

			msg.send("这次要调教哪只Bot呢 ~").keyboard(new Keyboard() {{

						FindIterable<UserBot> bots = botData.findByField("user",user.id);

						for (UserBot bot : bots) {

							newButtonLine("@" + bot.userName);

						}
						
						newButtonLine("/cancel");

					}}).exec();

			setPoint(user,POINT_SET_BOT);

		} else if (function.startsWith("del")) {

			if (botData.countByField("user",user.id) == 0) {

				msg.send("乃还没有创建Bot呢... 试试 /newbot ~").exec();

				return;

			}

			msg.send("这次要沙哪只Bot呢 ~").keyboard(new Keyboard() {{

						FindIterable<UserBot> bots = botData.findByField("user",user.id);

						for (UserBot bot : bots) {

							newButtonLine("@" + bot.userName);

						}
						
						newButtonLine("/cancel");

					}}).exec();

			setPoint(user,POINT_DEL_BOT);

		}

	}

	@Override
	public void onPoint(UserData user,Msg msg,PointStore.Point point) {

		if (POINT_NEW_BOT.equals(point.point)) {

			if (!msg.hasText()) {

				msg.send("请选择...").exec();

				return;

			}

			switch (msg.text()) {

				case ChatForward : newChatForwardBot(user,msg);break;

				default : {

						msg.send("没有这种Bot ~").exec();

					}

			}

		} else if (POINT_NEW_FORWARD_BOT.equals(point.point)) {

			finishNewBot(user,msg,ChatForward,msg.text());

		} else if (POINT_FINISH_BOT.equals(point.point)) {

			onBotFinish(user,msg,(FinishPoint)point.data);

		} else if (POINT_SET_BOT.equals(point.point)) {

			setBot(user,msg);

		} else if (POINT_SET_CHAT_BOT.equals(point.point)) {

			onSetChatBot(user,msg,(Long)point.data);

		} else if (POINT_DEL_BOT.equals(point.point)) {

			delBot(user,msg);

		} else if (POINT_CONFIRM_DEL.equals(point.point)) {

			onDelConfirm(user,msg,(Long)point.data);

		}

	}

	@Override
	public void points(LinkedList<String> points) {

		points.add(POINT_NEW_BOT);
		points.add(POINT_NEW_FORWARD_BOT);

		points.add(POINT_FINISH_BOT);

		points.add(POINT_SET_BOT);
		points.add(POINT_SET_CHAT_BOT);

		points.add(POINT_DEL_BOT);
		points.add(POINT_CONFIRM_DEL);

	}

	@Override
	public int target() {

		return Private;

	}

	static class FinishPoint {

		String type;
		String params[];

	}

	static final String POINT_FINISH_BOT = "bot,new,finish";

	static final String POINT_NEW_FORWARD_BOT = "bot,new,forward";

	void newChatForwardBot(UserData user,Msg msg) {

		msg.send("请输入将要创建的BOT的欢迎语 :","取消创建使用 /cancel").removeKeyboard().exec();

		msg.send("就像这样 : 这里是" + user.name() + "的私聊BOT，直接发消息给咱就可以了 ~").exec();

		setPoint(user,POINT_NEW_FORWARD_BOT);

	}

	void finishNewBot(UserData user,Msg msg,String type,String... params) {

		msg.send("现在输入 BotToken 这需要在 @BotFather 申请 ~ ","使用 /cancel 取消 ~").exec();

		FinishPoint finish = new FinishPoint();

		finish.type = type;

		finish.params = params;

		setPoint(user,POINT_FINISH_BOT,finish);

	}

	void onBotFinish(UserData user,Msg msg,FinishPoint finish) {

		msg.send("正在检查BotToken...").exec();

		if (botData.countByField("token",msg.text()) > 0) {
			
			msg.send("这个Token已经被使用了... 请移除原有BOT").exec();
			
			return;
			
		}
		
		User bot = new TelegramBot(msg.text()).execute(new GetMe()).user();

		if (bot == null) {

			msg.send("BotToken无效... 请重新输入 :","使用 /cancel 取消 ~").exec();

			return;

		}

		UserBot userBot = new UserBot();

		userBot.id = bot.id();
		userBot.token = msg.text();
		userBot.userName = bot.username();
		userBot.type = finish.type;
		
		userBot.params = new LinkedList<String>();
		userBot.params.add(finish.params[0]);

		userBot.user = user.id;

		botData.setById(userBot.id,userBot);
		
		msg.send("设置完成 正在启动 ~").exec();
		
		new ForwardBot(userBot.user,userBot.token,userBot.params.get(0)).silentStart();
		
		msg.send("启动完成 ~ 乃的Bot : @" + userBot.userName).exec();

		clearPoint(user);

	}

	void setBot(UserData user,Msg msg) {

		UserBot bot = botData.collection.find(and(eq("user",user.id),eq("userName",msg.text().substring(1)))).first();

		if (bot == null) {

			msg.send("没有这只Bot....").exec();

			return;

		}

		switch (bot.type) {

			case ChatForward : setChatBot(user,msg,bot);break;

		}

	}

	static final String POINT_SET_CHAT_BOT = "bot,set,chat";

	void setChatBot(UserData user,Msg msg,UserBot bot) {

		msg.send("好，这是一只私聊Bot,当前的欢迎语是 :",bot.params.get(0),"","现在发送新欢迎语 或使用 /cancel 取消 ~").removeKeyboard().exec();

		setPoint(user,POINT_SET_CHAT_BOT,bot.id);

	}

	void onSetChatBot(UserData user,Msg msg,Long botId) {

		if (!msg.hasText()) {

			msg.send("请输入欢迎语 :","使用 /cancel 取消 ~").exec();

			return;

		}
		
	    UserBot bot = botData.getById(botId);

		bot.params.set(0,msg.text());
		
		botData.setById(botId,bot);

		ForwardBot fwd = new ForwardBot(bot.user,bot.token,bot.params.get(0));
		
		BotServer.fragments.put(bot.token,fwd);
		
		clearPoint(user);
		
		msg.send("设置成功 ~").exec();

	}

	static final String POINT_CONFIRM_DEL = "bot,del,confirm";

	void delBot(UserData user,Msg msg) {

		UserBot bot = botData.collection.find(and(eq("user",user.id),eq("userName",msg.text().substring(1)))).first();

		if (bot == null) {

			msg.send("没有这只Bot....").exec();

			return;

		}

		msg.send("发送 确认 确认删除，其他内容为取消 ~").removeKeyboard().exec();

		setPoint(user,POINT_CONFIRM_DEL,bot.id);

	}

	void onDelConfirm(UserData user,Msg msg,Long botId) {

		if ("确认".equals(msg.text())) {

			UserBot bot = botData.getById(botId);

			new TelegramBot(bot.token).execute(new DeleteWebhook());
			
			BotServer.fragments.remove(bot.token);
			
			botData.deleteById(botId);
			
		
			msg.send("已经删除...").exec();

		} else {

			msg.send("取消成功...").exec();
			
		}
		
		clearPoint(user);

	}

}
