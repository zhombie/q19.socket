package kz.q19.socket.listener

import kz.q19.domain.model.keyboard.button.RateButton
import kz.q19.socket.model.Greeting

interface CallListener {
    fun onPendingUsersQueueCount(text: String? = null, count: Int)

    fun onNoOnlineCallAgents(text: String? = null): Boolean

    fun onCallAgentGreet(greeting: Greeting)

    fun onCallFeedback(text: String, rateButtons: List<RateButton>? = null)

    fun onLiveChatTimeout(text: String? = null, timestamp: Long): Boolean
    fun onUserRedirected(text: String? = null, timestamp: Long): Boolean
    fun onCallAgentDisconnected(text: String? = null, timestamp: Long): Boolean
}