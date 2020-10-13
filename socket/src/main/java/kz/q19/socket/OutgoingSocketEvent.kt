package kz.q19.socket

internal enum class OutgoingSocketEvent(val value: String) {
    INITIALIZE_CALL("initialize"),

    MESSAGE("message"),

    USER_DASHBOARD("user_dashboard"),

    USER_FEEDBACK("user_feedback"),

    USER_LANGUAGE("user_language"),

    USER_MESSAGE("user_message"),

    USER_LOCATION("user_location"),

    CONFIRM_FUZZY_TASK("confirm_fuzzy_task"),

    EXTERNAL("external"),

    FORM_INIT("form_init"),
    FORM_FINAL("form_final"),

    CANCEL("cancel"),

    CANCEL_PENDING_CALL("cancel_pending_call")
}