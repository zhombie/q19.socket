package kz.q19.socket.listener

internal class ListenerInfo {
    var socketStateListener: SocketStateListener? = null
    var basicListener: BasicListener? = null
    var dialogListener: DialogListener? = null
    var formListener: FormListener? = null
    var webRTCListener: WebRTCListener? = null
    var locationListener: LocationListener? = null

    fun clear() {
        socketStateListener = null
        basicListener = null
        dialogListener = null
        formListener = null
        webRTCListener = null
        locationListener = null
    }
}