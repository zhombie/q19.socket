package kz.q19.socket.repository

interface SocketStateRepository {
    fun connect(url: String)
    fun release()
}