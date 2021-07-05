package kz.q19.socket.listener

import kz.q19.socket.utils.Logger

internal class ListenerInfo {

    companion object {
        private val TAG = ListenerInfo::class.java.simpleName
    }

    var socketStateListener: SocketStateListener? = null
    var chatBotListener: ChatBotListener? = null
    var callListener: CallListener? = null
    var formListener: FormListener? = null
    var taskListener: TaskListener? = null
    var webRTCListener: WebRTCListener? = null
    var armListener: ARMListener? = null

    fun clear() {
        Logger.debug(TAG, "clear()")

        socketStateListener = null
        chatBotListener = null
        callListener = null
        formListener = null
        taskListener = null
        webRTCListener = null
        armListener = null
    }

}