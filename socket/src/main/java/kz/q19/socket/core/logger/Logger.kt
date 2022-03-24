package kz.q19.socket.core.logger

import android.util.Log
import kz.q19.socket.SocketClientConfig

internal object Logger {
    fun debug(tag: String, message: String) {
        if (SocketClientConfig.isLoggingEnabled()) {
            Log.d(tag, message)
        }
    }

    fun error(tag: String, message: String) {
        if (SocketClientConfig.isLoggingEnabled()) {
            Log.e(tag, message)
        }
    }

    fun error(tag: String, e: Exception) {
        if (SocketClientConfig.isLoggingEnabled()) {
            Log.d(tag, e.toString())
        }
    }
}