package kz.q19.socket.listener

import kz.q19.domain.model.RatingButton

interface DialogListener {
//    fun onCall(type: String, media: String, operator: String, instance: String)
    fun onOperatorGreet(fullName: String, photoUrl: String?, text: String)
    fun onFeedback(text: String, ratingButtons: List<RatingButton>)
    fun onChatTimeout(text: String, timestamp: Long): Boolean
    fun onOperatorDisconnected(text: String, timestamp: Long): Boolean
    fun onUserRedirected(text: String, timestamp: Long): Boolean
}