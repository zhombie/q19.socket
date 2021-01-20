package kz.q19.socket.listener

import kz.q19.domain.model.form.Form
import kz.q19.domain.model.message.Message

interface FormListener {
    fun onFormInit(form: Form)
    fun onFormFound(message: Message, form: Form): Boolean
    fun onFormFinal(trackId: String?, taskId: Long?, message: String?, success: Boolean)
}