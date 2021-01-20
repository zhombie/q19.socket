package kz.q19.socket.repository

import kz.q19.domain.model.media.Media

interface SocketChatRepository {
    fun requestParentCategories()
    fun requestCategories(parentId: Long)
    fun requestResponse(id: Long)

    fun sendUserMessage(message: String)
    fun sendUserMediaMessage(type: Media.Type, url: String)

    fun sendFuzzyTaskConfirmation(name: String, email: String, phone: String)
}