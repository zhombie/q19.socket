package kz.q19.socket.repository

import kz.q19.domain.model.Form
import org.json.JSONObject

interface SocketFormRepository {
    fun sendFormInitialize(formId: Long)
    fun sendFormFinalize(form: Form, sender: String? = null, extraFields: List<JSONObject>? = null)
}