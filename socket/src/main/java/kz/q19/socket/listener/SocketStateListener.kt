package kz.q19.socket.listener

interface SocketStateListener {
    fun onConnect()
    fun onDisconnect()
}