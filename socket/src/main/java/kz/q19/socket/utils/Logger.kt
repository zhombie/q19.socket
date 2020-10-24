package kz.q19.socket.utils

import android.util.Log

internal object Logger {
    fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    fun error(tag: String, e: Exception) {
        Log.d(tag, e.toString())
    }
}