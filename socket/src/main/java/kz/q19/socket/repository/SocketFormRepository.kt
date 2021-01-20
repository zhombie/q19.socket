package kz.q19.socket.repository

import kz.q19.domain.model.form.Form
import kz.q19.socket.model.Sender

interface SocketFormRepository {
    fun sendFormInitialize(formId: Long)
    fun sendFormFinalize(sender: Sender?, form: Form, extraFields: List<Form.Field>)
}