bind {
"io.kurumi.ntt.*",
"io.kurumi.ntt.model.*",
"io.kurumi.ntt.model.request.*",
"io.kurumi.ntt.twitter.*",
}

this = Launcher.INSTANCE

function send(chat_id,msg)

return Send.new(this,530055491,msg)

end

function print(msg)

send(530055491,msg(:exec()

end
