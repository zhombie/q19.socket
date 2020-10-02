package kz.q19.socket

import kz.q19.domain.model.*
import kz.q19.domain.model.webrtc.WebRTC

interface SocketRepository {
    fun setSocketStateListener(socketStateListener: SocketStateListener? = null)
    fun setGeneralListener(generalListener: GeneralListener? = null)
    fun setWebRTCListener(webRTCListener: WebRTCListener? = null)
    fun removeListeners()

    fun connect(url: String)
    fun release()

    fun initializeCall(callType: CallType, language: Language, scope: String? = null)

    fun getParentCategories()
    fun getCategories(parentId: Long)
    fun getResponse(id: Long)

    fun sendUserLanguage(language: Language)

    fun sendUserMessage(message: String)
    fun sendUserMediaMessage(attachmentType: Attachment.Type, url: String)

    fun sendUserFeedback(rating: Int, chatId: Long)

    fun sendMessage(webRTC: WebRTC? = null, action: Message.Action? = null)

    fun sendFuzzyTaskConfirmation(name: String, email: String, phone: String)

    fun sendExternal(callbackData: String? = null)

    fun sendFormInitialize(formId: Long)
    fun sendFormFinalize(form: Form)

    fun sendCancel()

    fun sendCancelPendingCall()
}