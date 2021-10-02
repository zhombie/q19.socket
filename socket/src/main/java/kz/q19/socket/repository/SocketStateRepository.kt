package kz.q19.socket.repository

import io.socket.client.IO
import okhttp3.OkHttpClient

interface SocketStateRepository {
    companion object {
        fun getDefaultOptions(): IO.Options = IO.Options().apply {
            reconnection = true
            reconnectionAttempts = 3
            reconnectionDelay = 1_000
            reconnectionDelayMax = 5_000
            randomizationFactor = 0.5
            timeout = 20_000
        }
    }

    fun create(
        url: String,
        options: IO.Options = getDefaultOptions(),
        okHttpClient: OkHttpClient? = null
    )

    fun connect()
    fun disconnect()
    fun release()
}