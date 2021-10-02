package kz.q19.socket.repository

interface SocketStateRepository {
    fun create(url: String, isSSLClientEnabled: Boolean = false)
    fun connect()
    fun disconnect()
    fun release()
}