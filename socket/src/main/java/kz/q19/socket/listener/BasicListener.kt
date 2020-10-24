package kz.q19.socket.listener

import kz.q19.domain.model.*

interface BasicListener {
    fun onPendingUsersQueueCount(text: String? = null, count: Int)
    fun onNoOnlineOperators(text: String): Boolean
    fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean
    fun onNoResultsFound(text: String, timestamp: Long): Boolean

    fun onMessage(message: Message)

    fun onCategories(categories: List<Category>)
}