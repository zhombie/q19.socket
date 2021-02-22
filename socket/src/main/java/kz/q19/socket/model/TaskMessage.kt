package kz.q19.socket.model

data class TaskMessage constructor(
    val notification: Notification,
    val message: String?,
    val task: Task
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