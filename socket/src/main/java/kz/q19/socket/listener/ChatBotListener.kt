package kz.q19.socket.listener

import kz.q19.socket.model.Category

interface ChatBotListener {
    fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean
    fun onNoResultsFound(text: String, timestamp: Long): Boolean
    fun onCategories(categories: List<Category>)
}