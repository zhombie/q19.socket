package kz.q19.socket.listener

interface SocketStateListener {
    fun onSocketConnect()
    fun onSocketDisconnect()
}