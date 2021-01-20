package kz.q19.socket.repository

import kz.q19.domain.model.language.Language

interface SocketRepository : SocketListenerRepository,
    SocketEventRegistrationRepository,
    SocketStateRepository,
    SocketFormRepository,
    SocketLocationRepository,
    SocketCallRepository,
    SocketChatRepository
{
    fun getId(): String?
    fun isConnected(): Boolean

    fun sendUserLanguage(language: Language)

    fun sendExternal(callbackData: String? = null)

    fun sendCancel()
}