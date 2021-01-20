package kz.q19.socket.listener

internal class ListenerInfo {
    var socketStateListener: SocketStateListener? = null
    var chatBotListener: ChatBotListener? = null
    var dialogListener: DialogListener? = null
    var formListener: FormListener? = null
    var webRTCListener: WebRTCListener? = null
    var armListener: ARMListener? = null

    fun clear() {
        socketStateListener = null
        chatBotListener = null
        dialogListener = null
        formListener = null
        webRTCListener = null
        armListener = null
    }
}