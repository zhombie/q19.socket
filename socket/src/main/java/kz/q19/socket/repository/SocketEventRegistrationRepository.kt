package kz.q19.socket.repository

interface SocketEventRegistrationRepository {
    fun registerAllEventListeners()
    fun unregisterAllEventListeners()

    fun registerSocketConnectEventListener()
    fun unregisterSocketConnectEventListener()

    fun registerMessageEventListener()
    fun unregisterMessageEventListener()

    fun registerChatBotDashboardEventListener()
    fun unregisterChatBotDashboardEventListener()

    fun registerUsersQueueEventListener()
    fun unregisterUsersQueueEventListener()

    fun registerCallAgentGreetEventListener()
    fun unregisterCallAgentGreetEventListener()

    fun registerCallAgentTypingEventListener()
    fun unregisterCallAgentTypingEventListener()

    fun registerCard102UpdateEventListener()
    fun unregisterCard102UpdateEventListener()

    fun registerUserDialogFeedbackEventListener()
    fun unregisterUserDialogFeedbackEventListener()

    fun registerFormInitializeEventListener()
    fun unregisterFormInitializeEventListener()

    fun registerFormFinalizeEventListener()
    fun unregisterFormFinalizeEventListener()

    fun registerSocketDisconnectEventListener()
    fun unregisterSocketDisconnectEventListener()
}