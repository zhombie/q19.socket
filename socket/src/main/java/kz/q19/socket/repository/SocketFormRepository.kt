package kz.q19.socket.repository

import kz.q19.domain.model.Form

interface SocketFormRepository {
    fun sendFormInitialize(formId: Long)
    fun sendFormFinalize(form: Form, sender: String? = null)
}