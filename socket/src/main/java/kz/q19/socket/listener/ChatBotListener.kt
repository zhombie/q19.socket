package kz.q19.socket.listener

import kz.q19.domain.model.message.Message
import kz.q19.socket.model.Category

interface ChatBotListener {
    fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean
    fun onNoResultsFound(text: String, timestamp: Long): Boolean
    // TODO: Segregate
    fun onMessage(message: Message)
    fun onCategories(categories: List<Category>)
}