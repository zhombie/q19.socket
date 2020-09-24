package kz.q19.socket

enum class OutgoingSocketEvent(val value: String) {
    INITIALIZE_CALL("initialize"),

    USER_DASHBOARD("user_dashboard"),

    USER_FEEDBACK("user_feedback"),

    USER_MESSAGE("user_message"),

    MESSAGE("message"),

    CONFIRM_FUZZY_TASK("confirm_fuzzy_task"),

    USER_LANGUAGE("user_language"),

    EXTERNAL("external"),

    FORM_INIT("form_init"),
    FORM_FINAL("form_final"),

    CANCEL("cancel"),

    CANCEL_PENDING_CALL("cancel_pending_call")
}