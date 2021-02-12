package kz.q19.socket.repository

interface SocketStateRepository {
    fun create(url: String)
    fun connect()
    fun disconnect()
    fun release()
}