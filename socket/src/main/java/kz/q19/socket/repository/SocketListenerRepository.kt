package kz.q19.socket.repository

import kz.q19.socket.listener.*

interface SocketListenerRepository {
    fun setSocketStateListener(listener: SocketStateListener?)
    fun setChatBotListener(listener: ChatBotListener?)
    fun setDialogListener(listener: DialogListener?)
    fun setFormListener(listener: FormListener?)
    fun setWebRTCListener(listener: WebRTCListener?)
    fun setARMListener(listener: ARMListener?)

    fun removeAllListeners()
}