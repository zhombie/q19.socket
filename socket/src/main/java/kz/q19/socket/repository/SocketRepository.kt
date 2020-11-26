package kz.q19.socket.repository

import kz.q19.domain.model.*
import kz.q19.domain.model.webrtc.WebRTC

interface SocketRepository : SocketListenerRepository,
    SocketStateRepository,
    SocketFormRepository,
    SocketLocationRepository
{
    fun isConnected(): Boolean
    fun getLastActiveTime(): Long

    @Deprecated(
        "Server does not support method. Migrate to the new method",
        replaceWith = ReplaceWith("initializeCall(callType, language, scope, topic)")
    )
    fun initializeCall(callType: CallType, language: Language, scope: String? = null)
    fun initializeCall(callType: CallType, language: Language, scope: String? = null, topic: String? = null)

    fun getParentCategories()
    fun getCategories(parentId: Long)
    fun getResponse(id: Long)

    fun sendUserLanguage(language: Language)

    fun sendUserMessage(message: String)
    fun sendUserMediaMessage(attachmentType: Attachment.Type, url: String)

    fun sendUserFeedback(rating: Int, chatId: Long)

    fun sendMessage(webRTC: WebRTC? = null, action: Message.Action? = null)
    fun sendMessage(id: String, location: Location)

    fun sendFuzzyTaskConfirmation(name: String, email: String, phone: String)

    fun sendExternal(callbackData: String? = null)

    fun sendCancel()

    fun sendCancelPendingCall()
}