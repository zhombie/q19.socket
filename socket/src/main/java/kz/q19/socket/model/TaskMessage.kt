package kz.q19.socket.model

import kz.q19.domain.model.media.Media

data class TaskMessage constructor(
    val id: Long,
    val notification: Notification,
    val task: Task,
    val text: String? = null,
    val media: Media? = null
) {

    data class Notification constructor(
        val title: String?,
        val url: String? = null
    )

    data class Task constructor(
        val id: Long,
        val trackId: String?
    )

}