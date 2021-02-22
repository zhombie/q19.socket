package kz.q19.socket.listener

internal class ListenerInfo {
    var socketStateListener: SocketStateListener? = null
    var chatBotListener: ChatBotListener? = null
    var callListener: CallListener? = null
    var formListener: FormListener? = null
    var taskListener: TaskListener? = null
    var webRTCListener: WebRTCListener? = null
    var armListener: ARMListener? = null

    fun clear() {
        socketStateListener = null
        chatBotListener = null
        callListener = null
        formListener = null
        taskListener = null
        webRTCListener = null
        armListener = null
    }
}