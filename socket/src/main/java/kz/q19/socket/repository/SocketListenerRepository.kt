package kz.q19.socket.repository

import kz.q19.socket.listener.*

interface SocketListenerRepository {
    fun setSocketStateListener(socketStateListener: SocketStateListener?)
    fun setBasicListener(basicListener: BasicListener?)
    fun setDialogListener(dialogListener: DialogListener?)
    fun setFormListener(formListener: FormListener?)
    fun setWebRTCListener(webRTCListener: WebRTCListener?)
    fun setLocationListener(locationListener: LocationListener?)

    fun removeAllListeners()
}