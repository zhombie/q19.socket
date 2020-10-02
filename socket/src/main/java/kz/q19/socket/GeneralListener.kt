package kz.q19.socket

import kz.q19.domain.model.*

interface GeneralListener {
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

    fun onMessage(message: Message)

    fun onCategories(categories: List<Category>)
}