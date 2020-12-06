package kz.q19.socket.repository

import kz.q19.domain.model.*
import kz.q19.domain.model.webrtc.WebRTCInfo

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
    fun initializeCall(
        callType: CallType,
        userId: Long,
        domain: String? = null,
        topic: String? = null,
        location: Location? = null,
        language: Language
    )

    fun getParentCategories()
    fun getCategories(parentId: Long)
    fun getResponse(id: Long)

    fun sendUserLanguage(language: Language)

    fun sendUserMessage(message: String)
    fun sendUserMediaMessage(type: Media.Type, url: String)

    fun sendUserFeedback(rating: Int, chatId: Long)

    fun sendMessage(webRTCInfo: WebRTCInfo? = null, action: Message.Action? = null)
    fun sendMessage(id: String, location: Location)

    fun sendFuzzyTaskConfirmation(name: String, email: String, phone: String)

    fun sendExternal(callbackData: String? = null)

    fun sendCancel()

    fun sendCancelPendingCall()
}