package kz.q19.socket

import kz.q19.domain.model.*

abstract class Listener : WebRTCListener() {
    abstract fun onConnect()
    abstract fun onDisconnect()

//    abstract fun onCall(type: String, media: String, operator: String, instance: String)
    abstract fun onOperatorGreet(fullName: String, photoUrl: String?, text: String)
    abstract fun onFormInit(form: Form)
    abstract fun onFormFinal(text: String)
    abstract fun onFeedback(text: String, ratingButtons: List<RatingButton>)
    abstract fun onPendingUsersQueueCount(text: String? = null, count: Int)
    abstract fun onNoOnlineOperators(text: String): Boolean
    abstract fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean
    abstract fun onNoResultsFound(text: String, timestamp: Long): Boolean
    abstract fun onChatTimeout(text: String, timestamp: Long): Boolean
    abstract fun onOperatorDisconnected(text: String, timestamp: Long): Boolean
    abstract fun onUserRedirected(text: String, timestamp: Long): Boolean

    abstract fun onTextMessage(
        text: String?,
        replyMarkup: Message.ReplyMarkup? = null,
        attachments: List<Attachment>? = null,
        form: Form? = null,
        timestamp: Long
    )
    abstract fun onAttachmentMessage(
        attachment: Attachment,
        replyMarkup: Message.ReplyMarkup? = null,
        timestamp: Long
    )

    abstract fun onCategories(categories: List<Category>)
}