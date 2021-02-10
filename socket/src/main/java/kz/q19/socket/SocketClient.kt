package kz.q19.socket

import android.Manifest
import androidx.annotation.RequiresPermission
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kz.q19.domain.model.*
import kz.q19.domain.model.call.CallType
import kz.q19.domain.model.form.Form
import kz.q19.domain.model.geo.Location
import kz.q19.domain.model.keyboard.Keyboard
import kz.q19.domain.model.keyboard.button.*
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.media.Media
import kz.q19.domain.model.message.CallAction
import kz.q19.domain.model.message.Message
import kz.q19.domain.model.message.QRTCAction
import kz.q19.domain.model.webrtc.*
import kz.q19.socket.event.SocketEvent
import kz.q19.socket.listener.*
import kz.q19.socket.model.*
import kz.q19.socket.repository.SocketRepository
import kz.q19.socket.utils.Logger
import kz.q19.utils.enums.findEnumBy
import kz.q19.utils.json.*
import org.json.JSONArray
import org.json.JSONObject

class SocketClient private constructor() : SocketRepository {

    companion object {
        private val TAG = SocketClient::class.java.simpleName

        @Volatile
        private var INSTANCE: SocketClient? = null

        fun getInstance(): SocketClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SocketClient().also { INSTANCE = it }
            }
        }
    }

    private var socket: Socket? = null

    private val language: String
        get() {
            val language = SocketClientConfig.getLanguage()
            return language.key
        }

    private val listenerInfo: ListenerInfo by lazy { ListenerInfo() }

    override fun setSocketStateListener(listener: SocketStateListener?) {
        Logger.debug(TAG, "setSocketStateListener() -> listener: $listener")

        listenerInfo.socketStateListener = listener
    }

    override fun setChatBotListener(listener: ChatBotListener?) {
        Logger.debug(TAG, "setChatBotListener() -> listener: $listener")

        listenerInfo.chatBotListener = listener
    }

    override fun setWebRTCListener(listener: WebRTCListener?) {
        Logger.debug(TAG, "setWebRTCListener() -> listener: $listener")

        listenerInfo.webRTCListener = listener
    }

    override fun setDialogListener(listener: DialogListener?) {
        Logger.debug(TAG, "setDialogListener() -> listener: $listener")

        listenerInfo.dialogListener = listener
    }

    override fun setFormListener(listener: FormListener?) {
        Logger.debug(TAG, "setFormListener() -> listener: $listener")

        listenerInfo.formListener = listener
    }

    override fun setARMListener(listener: ARMListener?) {
        Logger.debug(TAG, "setLocationListener() -> listener: $listener")

        listenerInfo.armListener = listener
    }

    override fun removeAllListeners() {
        Logger.debug(TAG, "removeAllListeners()")

        listenerInfo.clear()
    }

    override fun create(url: String) {
        Logger.debug(TAG, "create() -> url: $url")

        val options = IO.Options()
        options.reconnection = true
        options.reconnectionAttempts = 3
        options.reconnectionDelayMax = 3000L

        socket = IO.socket(url, options)
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    override fun connect() {
        Logger.debug(TAG, "connect()")

        socket?.connect()
    }

    override fun registerAllEventListeners(): Boolean {
        Logger.debug(TAG, "registerAllEventListeners()")

        return registerSocketConnectEventListener() &&
                registerMessageEventListener() &&
                registerUsersQueueEventListener() &&
                registerCallAgentGreetEventListener() &&
                registerCallAgentTypingEventListener() &&
                registerCard102UpdateEventListener() &&
                registerUserDialogFeedbackEventListener() &&
                registerFormInitializeEventListener() &&
                registerFormFinalizeEventListener() &&
                registerSocketDisconnectEventListener()
    }

    override fun unregisterAllEventListeners(): Boolean {
        Logger.debug(TAG, "unregisterAllEventListeners()")

        return if (socket == null || socket?.connected() == false) {
            false
        } else {
            socket?.off()
            true
        }
    }

    override fun registerSocketConnectEventListener(): Boolean {
        socket?.on(Socket.EVENT_CONNECT, onConnectListener)
        return true
    }

    override fun unregisterSocketConnectEventListener(): Boolean {
        socket?.off(Socket.EVENT_CONNECT, onConnectListener)
        return true
    }

    override fun registerMessageEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.MESSAGE, onMessageListener)

    override fun unregisterMessageEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.MESSAGE, onMessageListener)

    override fun registerChatBotDashboardEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.CATEGORY_LIST, onCategoryListListener)

    override fun unregisterChatBotDashboardEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.CATEGORY_LIST, onCategoryListListener)

    override fun registerUsersQueueEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.USER_QUEUE, onUserQueueListener)

    override fun unregisterUsersQueueEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.USER_QUEUE, onUserQueueListener)

    override fun registerCallAgentGreetEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.OPERATOR_GREET, onOperatorGreetListener)

    override fun unregisterCallAgentGreetEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.OPERATOR_GREET, onOperatorGreetListener)

    override fun registerCallAgentTypingEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.OPERATOR_TYPING, onOperatorTypingListener)

    override fun unregisterCallAgentTypingEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.OPERATOR_TYPING, onOperatorTypingListener)

    override fun registerCard102UpdateEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.CARD102_UPDATE, onCard102UpdateListener)

    override fun unregisterCard102UpdateEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.CARD102_UPDATE, onCard102UpdateListener)

    override fun registerLocationUpdateEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.LOCATION_UPDATE, onLocationUpdate)

    override fun unregisterLocationUpdateEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.LOCATION_UPDATE, onLocationUpdate)

    override fun registerUserDialogFeedbackEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.FEEDBACK, onFeedbackListener)

    override fun unregisterUserDialogFeedbackEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.FEEDBACK, onFeedbackListener)

    override fun registerFormInitializeEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.FORM_INIT, onFormInitListener)

    override fun unregisterFormInitializeEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.FORM_INIT, onFormInitListener)

    override fun registerFormFinalizeEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.FORM_FINAL, onFormFinalListener)

    override fun unregisterFormFinalizeEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.FORM_FINAL, onFormFinalListener)

    override fun registerSocketDisconnectEventListener(): Boolean {
        socket?.on(Socket.EVENT_DISCONNECT, onDisconnectListener)
        return true
    }

    override fun unregisterSocketDisconnectEventListener(): Boolean {
        socket?.off(Socket.EVENT_DISCONNECT, onDisconnectListener)
        return true
    }

    private fun registerEventListener(event: String, listener: Emitter.Listener): Boolean {
        Logger.debug(TAG, "registerEventListener() -> $event, $listener")
        return if (socket?.hasListeners(event) == true) {
            false
        } else {
            socket?.on(event, listener)
            true
        }
    }

    private fun unregisterEventListener(event: String, listener: Emitter.Listener): Boolean {
        Logger.debug(TAG, "unregisterEventListener() -> $event, $listener")
        return if (socket?.hasListeners(event) == true) {
            socket?.off(event, listener)
            true
        } else {
            false
        }
    }

    override fun release() {
        Logger.debug(TAG, "release()")

        socket?.off()
        socket?.disconnect()
        socket = null
    }

    override fun getId(): String? = socket?.id()

    override fun isConnected(): Boolean = socket?.connected() ?: false

    override fun sendCallInitialization(callInitialization: CallInitialization) {
        Logger.debug(TAG, "sendCallInitialization() -> $callInitialization")

        emit(SocketEvent.Outgoing.INITIALIZE, json {
            when (callInitialization.callType) {
                CallType.TEXT -> {
                    // Ignored
                }
                CallType.AUDIO -> {
                    put("media", "audio")
                }
                CallType.VIDEO -> {
                    put("media", "video")
                }
                else -> {
                    return@json
                }
            }

            putIfValueNotNull("user_id", callInitialization.userId)
            putIfValueNotNull("domain", callInitialization.domain)
            putIfValueNotNull("topic", callInitialization.topic)

            if (callInitialization.device != null) {
                put("device", json {
                    putIfValueNotNull("os", callInitialization.device.os)
                    putIfValueNotNull("os_ver", callInitialization.device.osVersion)
                    putIfValueNotNull("name", callInitialization.device.name)
                    putIfValueNotNull("mobile_operator", callInitialization.device.mobileOperator)
                    putIfValueNotNull("app_ver", callInitialization.device.appVersion)

                    if (callInitialization.device.battery != null) {
                        put("battery", json {
                            putIfValueNotNull("percentage", callInitialization.device.battery.percentage)
                            putIfValueNotNull("is_charging", callInitialization.device.battery.isCharging)
                            putIfValueNotNull("temperature", callInitialization.device.battery.temperature)
                        })
                    }
                })
            }

            if (callInitialization.location != null) {
                put("location", json {
                    put("lat", callInitialization.location.latitude)
                    put("lon", callInitialization.location.longitude)
                })
            }

            put("lang", callInitialization.language.key)
        }) {}
    }

    override fun getParentCategories() {
        Logger.debug(TAG, "getParentCategories()")

        getCategories(parentId = Category.NO_PARENT_ID)
    }

    override fun getCategories(parentId: Long) {
        Logger.debug(TAG, "getCategories() -> parentId: $parentId")

        emit(SocketEvent.Outgoing.USER_DASHBOARD, json {
            put("action", "get_category_list")
            put("parent_id", parentId)
            put("lang", language)
        }) {}
    }

    override fun getResponse(id: Long) {
        Logger.debug(TAG, "getResponse() -> id: $id")

        emit(SocketEvent.Outgoing.USER_DASHBOARD, json {
            put("action", "get_response")
            put("id", id)
            put("lang", language)
        }) {}
    }

    override fun sendUserLanguage(language: Language) {
        Logger.debug(TAG, "sendUserLanguage() -> language: $language")

        emit(SocketEvent.Outgoing.USER_LANGUAGE, json {
            put("language", language.key)
        }) {}
    }

    override fun sendUserMessage(message: String) {
        Logger.debug(TAG, "sendUserMessage() -> message: $message")

        val text = message.trim()

        emit(SocketEvent.Outgoing.USER_MESSAGE, json {
            put("text", text)
            put("lang", language)
        }) {}
    }

    override fun sendUserMediaMessage(type: Media.Type, url: String) {
        Logger.debug(TAG, "sendUserMediaMessage() -> type: $type, url: $url")

        emit(SocketEvent.Outgoing.USER_MESSAGE, json {
            put(type.key, url)
        }) {}
    }

    override fun sendUserDialogFeedback(rating: Int, chatId: Long) {
        Logger.debug(TAG, "sendUserDialogFeedback() -> rating: $rating, chatId: $chatId")

        emit(SocketEvent.Outgoing.USER_FEEDBACK, json {
            put("r", rating)
            put("chat_id", chatId)
        }) {}
    }

    override fun sendLocationUpdateSubscription() {
        Logger.debug(TAG, "sendLocationUpdateSubscription()")

        emit(SocketEvent.Outgoing.LOCATION_SUBSCRIBE) {}
    }

    override fun sendLocationUpdateUnsubscription() {
        Logger.debug(TAG, "sendLocationUpdateUnsubscription()")

        emit(SocketEvent.Outgoing.LOCATION_UNSUBSCRIBE) {}
    }

    override fun sendCallAction(action: CallAction) {
        Logger.debug(TAG, "sendCallAction() -> $action")

        emit(SocketEvent.Outgoing.MESSAGE, json {
            put("action", action.value)
        }) {}
    }

    override fun sendQRTCAction(action: QRTCAction) {
        Logger.debug(TAG, "sendQRTCAction() -> $action")

        emit(SocketEvent.Outgoing.MESSAGE, json {
            put("rtc", json {
                put("type", action.value)
            })
        }) {}
    }

    override fun sendLocalSessionDescription(sessionDescription: SessionDescription) {
        Logger.debug(TAG, "sendSessionDescription() -> $sessionDescription")

        emit(SocketEvent.Outgoing.MESSAGE, json {
            put("rtc", json {
                put("type", when (sessionDescription.type) {
                    SessionDescription.Type.OFFER -> "offer"
                    SessionDescription.Type.ANSWER -> "answer"
                })
                put("sdp", sessionDescription.description)
            })
        }) {}
    }

    override fun sendLocalIceCandidate(iceCandidate: IceCandidate) {
        Logger.debug(TAG, "sendIceCandidate() -> $iceCandidate")

        emit(SocketEvent.Outgoing.MESSAGE, json {
            put("rtc", json {
                put("type", "candidate")
                put("id", iceCandidate.sdpMid)
                put("label", iceCandidate.sdpMLineIndex)
                put("candidate", iceCandidate.sdp)
            })
        }) {}
    }

    override fun sendUserLocation(id: String, location: Location) {
        Logger.debug(TAG, "sendUserLocation() -> location: $location")

        emit(SocketEvent.Outgoing.MESSAGE, json {
            put("action", "location")
            put("id", id)
            put("provider", location.provider)
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("bearing", location.bearing)
            put("bearingAccuracyDegrees", location.bearingAccuracyDegrees)
            put("xAccuracyMeters", location.xAccuracyMeters)
            put("yAccuracyMeters", location.yAccuracyMeters)
            put("speed", location.speed)
            put("speedAccuracyMetersPerSecond", location.speedAccuracyMetersPerSecond)
        }) {}
    }

    override fun sendFuzzyTaskConfirmation(name: String, email: String, phone: String) {
        emit(SocketEvent.Outgoing.CONFIRM_FUZZY_TASK, json {
            put("name", name)
            put("email", email)
            put("phone", phone)
            put("res", '1')
        }) {}
    }

    override fun sendExternal(callbackData: String?) {
        emit(SocketEvent.Outgoing.EXTERNAL, json {
            put("callback_data", callbackData)
        }) {}
    }

    override fun sendFormInitialize(formId: Long) {
        emit(SocketEvent.Outgoing.FORM_INIT, json {
            put("form_id", formId)
        }) {}
    }

    override fun sendFormFinalize(sender: Sender?, form: Form, extraFields: List<Form.Field>) {
        emit(SocketEvent.Outgoing.FORM_FINAL, json {
            putIfValueNotNull("sender", sender?.get())

            put("form_id", form.id)

            val nodes = JSONArray()
            val fields = JSONObject()

            extraFields.forEach {
                fields.put(it.title, json { put(it.type.key, it.value) })
            }

            form.fields.forEach { field ->
                if (field.isFlexible) {
                    nodes.put(json {
                        put(field.type.key, field.value)
                        put(
                            "${field.type.key}_info",
                            json {
                                putIfValueNotNull("extension", field.info?.extension?.value)
                                putIfValueNotNull("width", field.info?.width)
                                putIfValueNotNull("height", field.info?.height)
                                putIfValueNotNull("duration", field.info?.duration)
                                putIfValueNotNull("date_added", field.info?.dateAdded)
                                putIfValueNotNull("date_modified", field.info?.dateModified)
                                putIfValueNotNull("date_taken", field.info?.dateTaken)
                                putIfValueNotNull("size", field.info?.size)
                            }
                        )
                    })
                } else {
                    val title = field.title
                    if (title.isNotBlank()) {
                        fields.put(title, json { put(field.type.key, field.value) })
                    }
                }
            }

            put("form_data", json {
                put("nodes", nodes)
                put("fields", fields)
            })
        }) {}
    }

    override fun sendCancel() {
        emit(SocketEvent.Outgoing.CANCEL) {}
    }

    override fun sendPendingCallCancellation() {
        emit(SocketEvent.Outgoing.CANCEL_PENDING_CALL) {}
    }

    private fun emit(event: String, jsonObject: JSONObject? = null, ack: (args: Array<Any>) -> Unit): Emitter? {
        return socket?.emit(
            event,
            jsonObject,
            Ack { args ->
                Logger.debug(TAG, "args: $args")
                ack(args)
            }
        )
    }

    private val onConnectListener by lazy {
        Emitter.Listener {
            Logger.debug(TAG, "event [${Socket.EVENT_CONNECT}]")

            listenerInfo.socketStateListener?.onSocketConnect()
        }
    }

    private val onFormInitListener by lazy {
        Emitter.Listener { args ->
//        Logger.debug(TAG, "event [${SocketEvent.Incoming.FORM_INIT}]: $args")

            if (args.size != 1) return@Listener

            val data = args[0] as? JSONObject? ?: return@Listener

            Logger.debug(TAG, "[${SocketEvent.Incoming.FORM_INIT}] data: $data")

            val formJson = data.getJSONObject("form")
            val formFieldsJsonArray = data.getJSONArray("form_fields")

            val fields = mutableListOf<Form.Field>()
            for (i in 0 until formFieldsJsonArray.length()) {
                val formFieldJson = formFieldsJsonArray[i]
                if (formFieldJson is JSONObject) {
                    fields.add(
                        Form.Field(
                            id = formFieldJson.getLong("id"),
                            title = formFieldJson.getStringOrNull("title") ?: continue,
                            prompt = formFieldJson.getStringOrNull("prompt"),
                            type = findEnumBy { it.key == formFieldJson.getString("type") } ?: continue,
                            defaultValue = formFieldJson.getStringOrNull("default"),
                            configs = null,
                            level = formFieldJson.optInt("level", -1),
                        )
                    )
                }
            }

            val form = Form(
                id = formJson.getLong("id"),
                title = formJson.getStringOrNull("title") ?: "",
                isFlexible = formJson.optInt("is_flex", -1) == 1,
                fields = fields,
                configs = Form.Configs()
            )

            Logger.debug(TAG, "listenerInfo.formListener: ${listenerInfo.formListener}")

            listenerInfo.formListener?.onFormInit(form)
        }
    }

    private val onFormFinalListener by lazy {
        Emitter.Listener { args ->
            Logger.debug(TAG, "event [${SocketEvent.Incoming.FORM_FINAL}]: $args")

            if (args.size != 1) return@Listener

            val data = args[0] as? JSONObject? ?: return@Listener

            Logger.debug(TAG, "[FORM_FINAL] data: $data")

            val taskJson = data.optJSONObject("task")
            val trackId = taskJson?.getStringOrNull("track_id")
            val taskId = taskJson?.getLongOrNull("task_id")
            val message = data.getStringOrNull("message")
            val success = data.optBoolean("success", false)

            listenerInfo.formListener?.onFormFinal(
                trackId = trackId,
                taskId = taskId,
                message = message,
                success = success
            )
        }
    }

    private val onOperatorGreetListener by lazy {
        Emitter.Listener { args ->
//        Logger.debug(TAG, "event [${SocketEvent.Incoming.OPERATOR_GREET}]: $args")

            if (args.size != 1) return@Listener

            val data = args[0] as? JSONObject? ?: return@Listener

            Logger.debug(TAG, "[${SocketEvent.Incoming.OPERATOR_GREET}] data: $data")

//        val name = data.optString("name")
            val fullName = data.optString("full_name")

            // Url path
            val photo = data.optString("photo")

            val text = data.optString("text")

            Logger.debug(TAG, "listenerInfo.dialogListener: ${listenerInfo.dialogListener}")

            listenerInfo.dialogListener?.onCallAgentGreet(fullName, photo, text)
        }
    }

    private val onOperatorTypingListener by lazy {
        Emitter.Listener {
            Logger.debug(TAG, "event [${SocketEvent.Incoming.OPERATOR_TYPING}]")
        }
    }

    private val onFeedbackListener by lazy {
        Emitter.Listener { args ->
//        Logger.debug(TAG, "event [${SocketEvent.Incoming.FEEDBACK}]: $args")

            if (args.size != 1) return@Listener

            val data = args[0] as? JSONObject? ?: return@Listener

            Logger.debug(TAG, "[${SocketEvent.Incoming.FEEDBACK}] data: $data")

            val buttonsJSONArray = data.optJSONArray("buttons")

            val text = data.optString("text")
            val chatId = data.optLong("chat_id", -1)

            if (buttonsJSONArray != null) {
                val rateButtons = mutableListOf<RateButton>()
                for (i in 0 until buttonsJSONArray.length()) {
                    val button = buttonsJSONArray[i]
                    if (button is JSONObject) {
                        val payload = button.optString("payload")
                        val (_, rating, _) = payload.split(":")
                        rateButtons.add(
                            RateButton(
                                text = button.optString("title"),
                                chatId = chatId,
                                rating = rating.toInt()
                            )
                        )
                    }
                }

                listenerInfo.dialogListener?.onDialogFeedback(text, rateButtons)
            } else {
                listenerInfo.dialogListener?.onDialogFeedback(text, null)
            }
        }
    }

    private val onUserQueueListener by lazy {
        Emitter.Listener { args ->
//        Logger.debug(TAG, "event [USER_QUEUE]: $args")

            if (args.size != 1) return@Listener

            val data = args[0] as? JSONObject? ?: return@Listener

//            Logger.debug(TAG, "[USER_QUEUE] data: $data")

            val count = data.getInt("count")
//            val channel = userQueue.getInt("channel")

            listenerInfo.dialogListener?.onPendingUsersQueueCount(count = count)
        }
    }

    private val onMessageListener by lazy {
        Emitter.Listener { args ->
//        Logger.debug(TAG, "event [${SocketEvent.Incoming.MESSAGE}]: $args")

            if (args.size != 1) return@Listener

            val data = args[0] as? JSONObject? ?: return@Listener

            Logger.debug(TAG, "[${SocketEvent.Incoming.MESSAGE}] data: $data")

            val id = data.getStringOrNull("id")?.trim()
            val text = data.getStringOrNull("text")?.trim()
            val noOnline = data.optBoolean("no_online")
            val noResults = data.optBoolean("no_results")
            val action = findEnumBy<CallAction> { it.value == data.getStringOrNull("action") }
            val time = data.optLong("time")
            val sender = data.getStringOrNull("sender")
            val from = data.getStringOrNull("from")
            val mediaJsonObject = data.optJSONObject("media")
            val rtcJsonObject = data.optJSONObject("rtc")
            val fuzzyTask = data.optBoolean("fuzzy_task")
            val attachmentsJsonArray = data.optJSONArray("attachments")
            val replyMarkupJsonObject = data.optJSONObject("reply_markup")
            val formJsonObject = data.optJSONObject("form")

            val inlineKeyboard = replyMarkupJsonObject?.optJSONArray("inline_keyboard")
            var keyboard: Keyboard? = null
            val rows = mutableListOf<List<Button>>()
            if (inlineKeyboard != null) {
                for (i in 0 until inlineKeyboard.length()) {
                    val row = inlineKeyboard[i]
                    if (row is JSONArray) {
                        val buttons = mutableListOf<Button>()
                        for (j in 0 until row.length()) {
                            val buttonJsonObject = row.get(j)
                            if (buttonJsonObject !is JSONObject) {
                                continue
                            }
                            val buttonText = buttonJsonObject.getString("text")
                            val callbackData = buttonJsonObject.getStringOrNull("callback_data")
                            val url = buttonJsonObject.getStringOrNull("url")
                            if (!callbackData.isNullOrBlank()) {
                                buttons.add(CallbackButton(text = buttonText, payload = callbackData))
                            } else if (!url.isNullOrBlank()) {
                                buttons.add(UrlButton(text = buttonText, url = url))
                            } else {
                                buttons.add(TextButton(text = buttonText))
                            }
                        }
                        rows.add(buttons)
                    }
                }

                keyboard = Keyboard(inline = true, buttons = rows)
            }

            if (noResults && from.isNullOrBlank() && sender.isNullOrBlank() && action == null && !text.isNullOrBlank()) {
                val isHandled = listenerInfo.chatBotListener?.onNoResultsFound(text, time)
                if (isHandled == true) return@Listener
            }

            if (fuzzyTask && !text.isNullOrBlank()) {
                val isHandled = listenerInfo.chatBotListener?.onFuzzyTaskOffered(text, time)
                if (isHandled == true) return@Listener
            }

            if (noOnline && !text.isNullOrBlank()) {
                val isHandled = listenerInfo.dialogListener?.onNoOnlineCallAgents(text)
                if (isHandled == true) return@Listener
            }

            if (action == CallAction.CHAT_TIMEOUT && !text.isNullOrBlank()) {
                val isHandled = listenerInfo.dialogListener?.onLiveChatTimeout(text, time)
                if (isHandled == true) return@Listener
            }

            if (action == CallAction.OPERATOR_DISCONNECT && !text.isNullOrBlank()) {
                val isHandled = listenerInfo.dialogListener?.onCallAgentDisconnected(text, time)
                if (isHandled == true) return@Listener
            }

            if (action == CallAction.REDIRECT && !text.isNullOrBlank()) {
                val isHandled = listenerInfo.dialogListener?.onUserRedirected(text, time)
                if (isHandled == true) return@Listener
            }

            if (rtcJsonObject != null) {
                when (findEnumBy<QRTCAction> { it.value == rtcJsonObject.getStringOrNull("type") }) {
                    QRTCAction.START -> {
                        when (action) {
                            CallAction.CALL_ACCEPT ->
                                listenerInfo.webRTCListener?.onCallAccept()
                            CallAction.CALL_REDIRECT ->
                                listenerInfo.webRTCListener?.onCallRedirect()
                            CallAction.CALL_REDIAL ->
                                listenerInfo.webRTCListener?.onCallRedial()
                            else -> {
                            }
                        }
                    }
                    QRTCAction.PREPARE ->
                        listenerInfo.webRTCListener?.onCallPrepare()
                    QRTCAction.READY ->
                        listenerInfo.webRTCListener?.onCallReady()
                    QRTCAction.OFFER -> {
                        val sdp = rtcJsonObject.getString("sdp")

                        listenerInfo.webRTCListener?.onCallOffer(
                            SessionDescription(
                                type = SessionDescription.Type.OFFER,
                                description = sdp
                            )
                        )
                    }
                    QRTCAction.ANSWER -> {
                        val sdp = rtcJsonObject.getString("sdp")

                        listenerInfo.webRTCListener?.onCallAnswer(
                            SessionDescription(
                                type = SessionDescription.Type.ANSWER,
                                description = sdp
                            )
                        )
                    }
                    QRTCAction.CANDIDATE ->
                        listenerInfo.webRTCListener?.onRemoteIceCandidate(
                            IceCandidate(
                                sdpMid = rtcJsonObject.getString("id"),
                                sdpMLineIndex = rtcJsonObject.getInt("label"),
                                sdp = rtcJsonObject.getString("candidate")
                            )
                        )
                    QRTCAction.HANGUP ->
                        listenerInfo.webRTCListener?.onPeerHangupCall()
                    else -> {
                        Logger.error(TAG, "Unsupported type for: $rtcJsonObject")
                    }
                }
                return@Listener
            }

            if (!data.isNull("queued")) {
                val queued = data.optInt("queued")
                listenerInfo.dialogListener?.onPendingUsersQueueCount(text, queued)
            }

            val attachments = mutableListOf<Media>()
            if (attachmentsJsonArray != null) {
                for (i in 0 until attachmentsJsonArray.length()) {
                    val attachment = attachmentsJsonArray[i]
                    if (attachment is JSONObject) {
                        attachments.add(
                            Media(
                                id = -1,
                                title = attachment.getStringOrNull("title"),
                                extension = findEnumBy { it.value == attachment.getStringOrNull("ext") },
                                type = findEnumBy { it.key == attachment.getStringOrNull("type") },
                                urlPath = attachment.getStringOrNull("url")
                            )
                        )
                    } else {
                        Logger.debug(TAG, "Unsupported type of message attachment")
                    }
                }
            }

            var media: Media? = null
            if (mediaJsonObject != null) {
                val image = mediaJsonObject.getStringOrNull("image")
                val audio = mediaJsonObject.getStringOrNull("audio")
                val video = mediaJsonObject.getStringOrNull("video")
                val document = mediaJsonObject.getStringOrNull("document")
                val file = mediaJsonObject.getStringOrNull("file")

                val name = mediaJsonObject.getStringOrNull("name")
                val ext = mediaJsonObject.getStringOrNull("ext")

                val pair = if (!ext.isNullOrBlank()) {
                    if (!image.isNullOrBlank()) {
                        Media.Type.IMAGE to image
                    } else if (!audio.isNullOrBlank()) {
                        Media.Type.AUDIO to audio
                    } else if (!video.isNullOrBlank()) {
                        Media.Type.VIDEO to video
                    } else if (!document.isNullOrBlank()) {
                        Media.Type.DOCUMENT to document
                    } else if (!file.isNullOrBlank()) {
                        Media.Type.FILE to file
                    } else {
                        null
                    }
                } else {
                    null
                }

                media = Media(
                    id = -1,
                    title = name,
                    extension = findEnumBy { it.value == ext },
                    urlPath = pair?.second,
                    type = pair?.first
                )
            }

            val message = Message.Builder()
                .setId(id)
                .setType(Message.Type.INCOMING)
                .setText(text)
                .setKeyboard(keyboard)
                .setMedia(media)
                .setAttachments(attachments)
                .setCreatedAt(time)
                .build()

            if (formJsonObject != null && formJsonObject.has("id")) {
                val form = Form(
                    id = formJsonObject.optLong("id"),
                    title = formJsonObject.getStringOrNull("title") ?: "",
                    prompt = formJsonObject.getStringOrNull("prompt"),
                    fields = emptyList()
                )
                if (listenerInfo.formListener?.onFormFound(message, form) == true) {
                    return@Listener
                }
            }

            listenerInfo.chatBotListener?.onMessage(message = message)
        }
    }

    private val onCategoryListListener by lazy {
        Emitter.Listener { args ->
//        Logger.debug(TAG, "event [${SocketEvent.Incoming.CATEGORY_LIST}]: $args")

            if (args.size != 1) return@Listener

            val data = args[0] as? JSONObject? ?: return@Listener

            Logger.debug(TAG, "[${SocketEvent.Incoming.CATEGORY_LIST}] data: $data")

            val categoryListJSONArray = data.optJSONArray("category_list") ?: return@Listener

            fun parse(jsonObject: JSONObject): Category? {
                return Category(
                    id = jsonObject.getLongOrNull("id") ?: return null,
                    title = jsonObject.getStringOrNull("title")?.trim(),
                    language = findEnumBy { it.value == jsonObject.optLong("lang") } ?: Language.ID.RU,
                    parentId = jsonObject.getLongOrNull("parent_id") ?: Category.NO_PARENT_ID,
                    photo = jsonObject.getStringOrNull("photo"),
                    responses = jsonObject.getAsMutableList("responses"),
                    config = Category.Config(
                        jsonObject.getObjectOrNull("config")?.getIntOrNull("order") ?: 0
                    )
                )
            }

            val categories = mutableListOf<Category>()
            for (i in 0 until categoryListJSONArray.length()) {
                val categoryJSONObject = categoryListJSONArray[i]
                if (categoryJSONObject is JSONObject) {
                    val parsed = parse(categoryJSONObject)
                    categories.add(parsed ?: continue)
                }
            }

            listenerInfo.chatBotListener?.onCategories(categories.sortedBy { it.config?.order })
        }
    }

    private val onCard102UpdateListener by lazy {
        Emitter.Listener { args ->
            Logger.debug(TAG, "event [${SocketEvent.Incoming.CARD102_UPDATE}]: $args")

            if (args.size != 1) return@Listener

            val data = args[0] as? JSONObject? ?: return@Listener

            val status = data.getIntOrNull("status") ?: -1

            val card102Status = Card102Status(status)

            if (card102Status != null) {
                listenerInfo.armListener?.onCard102Update(card102Status)
            } else {
                Logger.debug(TAG, "Incorrect/unsupported Card102 status: $status")
            }
        }
    }

    private val onLocationUpdate by lazy {
        Emitter.Listener { args ->
//            Logger.debug(TAG, "event [${SocketEvent.Incoming.LOCATION_UPDATE}]")

            // [{"Gps_Code":7170891,"X":71.4771061686837,"Y":51.1861201686837},{"Gps_Code":7171196,"X":71.43119816868371,"Y":51.1138291686837},{"Gps_Code":7170982,"X":71.5110101686837,"Y":51.1387631686837}]

            Logger.debug(TAG, "event [${SocketEvent.Incoming.LOCATION_UPDATE}] args: ${args.contentToString()}")

            val data = args[0] as? JSONArray? ?: return@Listener

            Logger.debug(TAG, "event [${SocketEvent.Incoming.LOCATION_UPDATE}] data: $data")

            val locationUpdates = mutableListOf<LocationUpdate>()
            for (i in 0 until data.length()) {
                (data[i] as? JSONObject?)?.run {
                    val gpsCode = optLong("Gps_Code", -1)
                    val x = optDouble("X", -1.0)
                    val y = optDouble("Y", -1.0)

                    locationUpdates.add(LocationUpdate(gpsCode, x, y))
                }
            }

            listenerInfo.armListener?.onLocationUpdate(locationUpdates)
        }
    }

    private val onDisconnectListener by lazy {
        Emitter.Listener {
            Logger.debug(TAG, "event [${Socket.EVENT_DISCONNECT}]")

            listenerInfo.socketStateListener?.onSocketDisconnect()
        }
    }

}