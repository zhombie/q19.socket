package kz.q19.socket.listener

import kz.q19.domain.model.keyboard.button.RateButton

interface DialogListener {
    fun onPendingUsersQueueCount(text: String? = null, count: Int)

    fun onNoOnlineCallAgents(text: String): Boolean

    fun onCallAgentGreet(fullName: String, photoUrl: String? = null, text: String)

    fun onDialogFeedback(text: String, rateButtons: List<RateButton>? = null)

    fun onLiveChatTimeout(text: String, timestamp: Long): Boolean
    fun onUserRedirected(text: String, timestamp: Long): Boolean
    fun onCallAgentDisconnected(text: String, timestamp: Long): Boolean
}