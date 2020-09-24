package kz.q19.socket

import kz.q19.domain.model.*

interface Listener : WebRTCListener {
    fun onConnect()

//    fun onCall(type: String, media: String, operator: String, instance: String)
    fun onOperatorGreet(fullName: String, photoUrl: String?, text: String)
    fun onFormInit(form: Form)
    fun onFormFinal(text: String)
    fun onFeedback(text: String, ratingButtons: List<RatingButton>)
    fun onPendingUsersQueueCount(text: String? = null, count: Int)
    fun onNoOnlineOperators(text: String): Boolean
    fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean
    fun onNoResultsFound(text: String, timestamp: Long): Boolean
    fun onChatTimeout(text: String, timestamp: Long): Boolean
    fun onOperatorDisconnected(text: String, timestamp: Long): Boolean
    fun onUserRedirected(text: String, timestamp: Long): Boolean

    fun onTextMessage(
        text: String?,
        replyMarkup: Message.ReplyMarkup? = null,
        attachments: List<Attachment>? = null,
        form: Form? = null,
        timestamp: Long
    )
    fun onAttachmentMessage(
        attachment: Attachment,
        replyMarkup: Message.ReplyMarkup? = null,
        timestamp: Long
    )

    fun onCategories(categories: List<Category>)

    fun onDisconnect()
}