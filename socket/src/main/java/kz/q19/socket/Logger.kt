package kz.q19.socket

import android.util.Log

internal object Logger {
    fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }
}