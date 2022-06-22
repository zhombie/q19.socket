package kz.q19.socket.model

data class Greeting constructor(
    val callAgent: CallAgent,
    val text: String
) {

    data class CallAgent constructor(
        val name: String,
        val fullName: String,
        val photoUrl: String? = null,
        val audioStreamEnabled: Boolean,
        val videoStreamEnabled: Boolean
    )

}
