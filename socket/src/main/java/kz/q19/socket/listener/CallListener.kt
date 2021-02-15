package kz.q19.socket.listener

import kz.q19.domain.model.keyboard.button.RateButton

interface CallListener {
    fun onPendingUsersQueueCount(text: String? = null, count: Int)

    fun onNoOnlineCallAgents(text: String? = null): Boolean

    fun onCallAgentGreet(fullName: String, photoUrl: String? = null, text: String)

    fun onCallFeedback(text: String, rateButtons: List<RateButton>? = null)

    fun onLiveChatTimeout(text: String? = null, timestamp: Long): Boolean
    fun onUserRedirected(text: String? = null, timestamp: Long): Boolean
    fun onCallAgentDisconnected(text: String? = null, timestamp: Long): Boolean
}