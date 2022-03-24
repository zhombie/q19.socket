package kz.q19.socket.emitter

import io.socket.emitter.Emitter
import org.json.JSONObject

fun interface JSONObjectListener : Emitter.Listener {
    override fun call(vararg args: Any?) {
        if (args.size == 1) {
            (args.first() as? JSONObject)?.let { call(it) }
        }
    }

    fun call(jsonObject: JSONObject)
}