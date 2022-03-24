package kz.q19.socket.emitter

import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject

fun interface JSONArrayListener : Emitter.Listener {
    override fun call(vararg args: Any?) {
        if (args.size == 1) {
            (args.first() as? JSONArray)?.let { call(it) }
        }
    }

    fun call(jsonArray: JSONArray)
}