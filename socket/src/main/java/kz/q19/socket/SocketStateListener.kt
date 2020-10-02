package kz.q19.socket

interface SocketStateListener {
    fun onConnect()
    fun onDisconnect()
}