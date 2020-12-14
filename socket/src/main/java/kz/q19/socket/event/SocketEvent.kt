package kz.q19.socket.event

internal sealed class SocketEvent {

    object Incoming : SocketEvent() {
//        const val CALL = "call"

        const val CARD102_UPDATE = "card102_update"

        const val CATEGORY_LIST = "category_list"

        const val FEEDBACK = "feedback"

        const val FORM_INIT = "form_init"
        const val FORM_FINAL = "form_final"

        const val LOCATION_UPDATE = "location_update"

        const val MESSAGE = "message"

        const val OPERATOR_GREET = "operator_greet"

        const val OPERATOR_TYPING = "operator_typing"

        const val USER_QUEUE = "user_queue"
    }

    object Outgoing : SocketEvent() {
        const val CANCEL = "cancel"

        const val CANCEL_PENDING_CALL = "cancel_pending_call"

        const val CONFIRM_FUZZY_TASK = "confirm_fuzzy_task"

        const val EXTERNAL = "external"

        const val FORM_INIT = "form_init"
        const val FORM_FINAL = "form_final"

        const val INITIALIZE = "initialize"

        const val LOCATION_SUBSCRIBE = "location_subscribe"

        const val MESSAGE = "message"

        const val USER_DASHBOARD = "user_dashboard"

        const val USER_FEEDBACK = "user_feedback"

        const val USER_LANGUAGE = "user_language"

        const val USER_LOCATION = "user_location"

        const val USER_MESSAGE = "user_message"
    }

}