package kz.q19.socket.listener

import kz.q19.domain.model.message.Category
import kz.q19.domain.model.message.Message

interface ChatBotListener {
    fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean
    fun onNoResultsFound(text: String, timestamp: Long): Boolean
    fun onMessage(message: Message)
    fun onCategories(categories: List<Category>)
}