package kz.q19.socket

import android.Manifest
import androidx.annotation.RequiresPermission
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kz.garage.chat.model.Entity
import kz.garage.chat.model.Message
import kz.garage.chat.model.reply_markup.InlineReplyMarkup
import kz.garage.chat.model.reply_markup.ReplyMarkup
import kz.garage.chat.model.reply_markup.button.Button
import kz.garage.chat.model.reply_markup.button.CallbackButton
import kz.garage.chat.model.reply_markup.button.TextButton
import kz.garage.chat.model.reply_markup.button.URLButton
import kz.garage.file.extension.Extension
import kz.garage.file.extension.Extensions
import kz.garage.json.*
import kz.garage.kotlin.enums.findEnumBy
import kz.garage.multimedia.store.model.*
import kz.q19.domain.model.*
import kz.q19.domain.model.call.type.CallType
import kz.q19.domain.model.content.ContentType
import kz.q19.domain.model.content.asContentType
import kz.q19.domain.model.form.Form
import kz.q19.domain.model.geo.Location
import kz.q19.domain.model.keyboard.button.RateButton
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.message.call.CallAction
import kz.q19.domain.model.message.qrtc.QRTCAction
import kz.q19.domain.model.webrtc.*
import kz.q19.socket.core.logger.Logger
import kz.q19.socket.emitter.JSONArrayListener
import kz.q19.socket.emitter.JSONObjectListener
import kz.q19.socket.event.SocketEvent
import kz.q19.socket.listener.*
import kz.q19.socket.model.*
import kz.q19.socket.repository.SocketRepository
import okhttp3.OkHttpClient
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

    override fun setCoreListener(listener: CoreListener?) {
        Logger.debug(TAG, "setChatBotListener() -> listener: $listener")
        listenerInfo.coreListener = listener
    }

    override fun setChatBotListener(listener: ChatBotListener?) {
        Logger.debug(TAG, "setChatBotListener() -> listener: $listener")
        listenerInfo.chatBotListener = listener
    }

    override fun setWebRTCListener(listener: WebRTCListener?) {
        Logger.debug(TAG, "setWebRTCListener() -> listener: $listener")
        listenerInfo.webRTCListener = listener
    }

    override fun setCallListener(listener: CallListener?) {
        Logger.debug(TAG, "setCallListener() -> listener: $listener")
        listenerInfo.callListener = listener
    }

    override fun setFormListener(listener: FormListener?) {
        Logger.debug(TAG, "setFormListener() -> listener: $listener")
        listenerInfo.formListener = listener
    }

    override fun setARMListener(listener: ARMListener?) {
        Logger.debug(TAG, "setLocationListener() -> listener: $listener")
        listenerInfo.armListener = listener
    }

    override fun setTaskListener(listener: TaskListener?) {
        Logger.debug(TAG, "setTaskListener() -> listener: $listener")
        listenerInfo.taskListener = listener
    }

    override fun removeAllListeners() {
        Logger.debug(TAG, "removeAllListeners()")
        listenerInfo.clear()
    }

    override fun create(url: String, options: IO.Options, okHttpClient: OkHttpClient?) {
        Logger.debug(TAG, "create() -> $url, $options, $okHttpClient")

        if (okHttpClient != null) {
            IO.setDefaultOkHttpCallFactory(okHttpClient)
            IO.setDefaultOkHttpWebSocketFactory(okHttpClient)

            Logger.debug(TAG, "create() -> callFactory: ${options.callFactory}")
            if (options.callFactory == null) {
                options.callFactory = okHttpClient
            }

            Logger.debug(TAG, "create() -> webSocketFactory: ${options.webSocketFactory}")
            if (options.webSocketFactory == null) {
                options.webSocketFactory = okHttpClient
            }
        }

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
                registerUserCallFeedbackEventListener() &&
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

    override fun registerUserCallFeedbackEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.FEEDBACK, onFeedbackListener)

    override fun unregisterUserCallFeedbackEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.FEEDBACK, onFeedbackListener)

    override fun registerFormInitializeEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.FORM_INIT, onFormInitListener)

    override fun unregisterFormInitializeEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.FORM_INIT, onFormInitListener)

    override fun registerFormFinalizeEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.FORM_FINAL, onFormFinalListener)

    override fun unregisterFormFinalizeEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.FORM_FINAL, onFormFinalListener)

    override fun registerTaskMessageEventListener(): Boolean =
        registerEventListener(SocketEvent.Incoming.TASK_MESSAGE, onTaskMessageListener)

    override fun unregisterTaskMessageEventListener(): Boolean =
        unregisterEventListener(SocketEvent.Incoming.TASK_MESSAGE, onTaskMessageListener)

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

    override fun disconnect() {
        Logger.debug(TAG, "disconnect()")

        socket?.disconnect()
    }

    override fun release() {
        socket?.off()
        socket = null
    }

    override fun getId(): String? = socket?.id()

    override fun isConnected(): Boolean = socket?.connected() ?: false

    override fun sendCallInitialization(callInitialization: CallInitialization) {
        Logger.debug(TAG, "sendCallInitialization() -> $callInitialization")

        emit(SocketEvent.Outgoing.INITIALIZE, jsonObject {
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
                else -> return@jsonObject
            }

            putIfValueNotNull("user_id", callInitialization.userId)
            putIfValueNotNull("domain", callInitialization.domain)
            putIfValueNotNull("topic", callInitialization.topic)

            if (callInitialization.location != null) {
                put("location", jsonObject {
                    put("lat", callInitialization.location.latitude)
                    put("lon", callInitialization.location.longitude)
                })
            }

            if (callInitialization.device != null) {
                put("device", jsonObject {
                    putIfValueNotNull("os", callInitialization.device.os)
                    putIfValueNotNull("os_ver", callInitialization.device.osVersion)
                    putIfValueNotNull("name", callInitialization.device.name)
                    putIfValueNotNull("mobile_operator", callInitialization.device.mobileOperator)
                    putIfValueNotNull("app_ver", callInitialization.device.appVersion)

                    if (callInitialization.device.battery != null) {
                        put("battery", jsonObject {
                            putIfValueNotNull("percentage", callInitialization.device.battery.percentage)
                            putIfValueNotNull("is_charging", callInitialization.device.battery.isCharging)
                            putIfValueNotNull("temperature", callInitialization.device.battery.temperature)
                        })
                    }
                })
            }

            putIfValueNotNull("first_name", callInitialization.firstName)
            putIfValueNotNull("last_name", callInitialization.lastName)
            putIfValueNotNull("patronymic", callInitialization.patronymic)
            putIfValueNotNull("iin", callInitialization.iin)
            putIfValueNotNull("phone", callInitialization.phone)

            putIfValueNotNull("service_code", callInitialization.serviceCode)

            putIfValueNotNull("action", callInitialization.action)

            if (callInitialization.language != null) {
                put("lang", callInitialization.language.key)
            }
        }) {}
    }

    override fun getParentCategories() {
        Logger.debug(TAG, "getParentCategories()")

        getCategories(parentId = Category.NO_PARENT_ID)
    }

    override fun getCategories(parentId: Long) {
        Logger.debug(TAG, "getCategories() -> parentId: $parentId")

        emit(SocketEvent.Outgoing.USER_DASHBOARD, jsonObject {
            put("action", "get_category_list")
            put("parent_id", parentId)
            put("lang", language)
        }) {}
    }

    override fun getResponse(id: Long) {
        Logger.debug(TAG, "getResponse() -> id: $id")

        emit(SocketEvent.Outgoing.USER_DASHBOARD, jsonObject {
            put("action", "get_response")
            put("id", id)
            put("lang", language)
        }) {}
    }

    override fun sendUserLanguage(language: Language) {
        Logger.debug(TAG, "sendUserLanguage() -> language: $language")

        emit(SocketEvent.Outgoing.USER_LANGUAGE, jsonObject {
            put("language", language.key)
        }) {}
    }

    override fun sendUserTextMessage(message: String) {
        Logger.debug(TAG, "sendUserMessage() -> message: $message")

        emit(SocketEvent.Outgoing.USER_MESSAGE, jsonObject {
            put("text", message.trim())
            put("lang", language)
        }) {}
    }

    override fun sendUserRichMessage(content: Content): Boolean {
        val type = content.asContentType()?.key
        val url = content.remoteFile?.url

        if (type.isNullOrBlank() || url.isNullOrBlank()) return false

        emit(SocketEvent.Outgoing.USER_MESSAGE, jsonObject {
            put(type, url)
        }) {}

        return true
    }

    override fun sendUserCallFeedback(chatId: Long, rating: Int) {
        Logger.debug(TAG, "sendUserCallFeedback() -> chatId: $chatId, rating: $rating")

        emit(SocketEvent.Outgoing.USER_FEEDBACK, jsonObject {
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

        emit(SocketEvent.Outgoing.MESSAGE, jsonObject {
            put("action", action.value)
        }) {}
    }

    override fun sendQRTCAction(action: QRTCAction) {
        Logger.debug(TAG, "sendQRTCAction() -> $action")

        emit(SocketEvent.Outgoing.MESSAGE, jsonObject {
            put("rtc", jsonObject {
                put("type", action.value)
            })
        }) {}
    }

    override fun sendLocalSessionDescription(sessionDescription: SessionDescription) {
        Logger.debug(TAG, "sendSessionDescription() -> $sessionDescription")

        emit(SocketEvent.Outgoing.MESSAGE, jsonObject {
            put("rtc", jsonObject {
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

        emit(SocketEvent.Outgoing.MESSAGE, jsonObject {
            put("rtc", jsonObject {
                put("type", "candidate")
                put("id", iceCandidate.sdpMid)
                put("label", iceCandidate.sdpMLineIndex)
                put("candidate", iceCandidate.sdp)
            })
        }) {}
    }

    override fun sendUserLocation(id: String, location: Location) {
        Logger.debug(TAG, "sendUserLocation() -> location: $location")

        emit(SocketEvent.Outgoing.MESSAGE, jsonObject {
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
        emit(SocketEvent.Outgoing.CONFIRM_FUZZY_TASK, jsonObject {
            put("name", name)
            put("email", email)
            put("phone", phone)
            put("res", '1')
        }) {}
    }

    override fun sendExternal(callbackData: String?) {
        emit(SocketEvent.Outgoing.EXTERNAL, jsonObject {
            put("callback_data", callbackData)
        }) {}
    }

    override fun sendFormInitialize(formId: Long) {
        emit(SocketEvent.Outgoing.FORM_INIT, jsonObject {
            put("form_id", formId)
        }) {}
    }

    override fun sendFormFinalize(sender: Sender?, form: Form, extraFields: List<Form.Field>) {
        emit(SocketEvent.Outgoing.FORM_FINAL, jsonObject {
            putIfValueNotNull("sender", sender?.get())

            put("form_id", form.id)

            val nodes = JSONArray()
            val fields = JSONObject()

            extraFields.forEach {
                fields.put(it.title, jsonObject { put(it.type.key, it.value) })
            }

            form.fields.forEach { field ->
                if (field.isFlexible) {
                    nodes.put(jsonObject {
                        put(field.type.key, field.value)
                        put(
                            "${field.type.key}_info",
                            jsonObject {
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
                        fields.put(title, jsonObject { put(field.type.key, field.value) })
                    }
                }
            }

            put("form_data", jsonObject {
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

    private fun emit(
        event: String,
        jsonObject: JSONObject? = null,
        ack: (args: Array<Any>) -> Unit
    ): Emitter? =
        socket?.emit(
            event,
            jsonObject,
            Ack { args ->
                Logger.debug(TAG, "call() -> args: $args")
                ack(args)
            }
        )

    private val onConnectListener by lazy {
        Emitter.Listener {
            Logger.debug(TAG, "event [${Socket.EVENT_CONNECT}]")

            listenerInfo.socketStateListener?.onSocketConnect()
        }
    }

    private val onFormInitListener by lazy {
        JSONObjectListener { jsonObject ->
            Logger.debug(TAG, "[${SocketEvent.Incoming.FORM_INIT}] jsonObject: $jsonObject")

            val formJSONObject = jsonObject.getJSONObject("form")

            val fields = jsonObject.getJSONArrayOrNull("form_fields")?.mapNotNull<JSONObject, Form.Field> {
                Form.Field(
                    id = it.getLong("id"),
                    title = it.getStringOrNull("title") ?: return@mapNotNull null,
                    prompt = it.getStringOrNull("prompt"),
                    type = findEnumBy { type -> type.key == it.getString("type") } ?: return@mapNotNull null,
                    defaultValue = it.getStringOrNull("default"),
                    configs = null,
                    level = it.optInt("level", -1),
                )
            }

            val form = Form(
                id = formJSONObject.getLong("id"),
                title = formJSONObject.getStringOrNull("title") ?: "",
                isFlexible = formJSONObject.optInt("is_flex", -1) == 1,
                fields = fields ?: emptyList(),
                configs = Form.Configs()
            )

            Logger.debug(TAG, "listenerInfo.formListener: ${listenerInfo.formListener}")

            listenerInfo.formListener?.onFormInit(form)
        }
    }

    private val onFormFinalListener by lazy {
        JSONObjectListener { jsonObject ->
            Logger.debug(TAG, "[${SocketEvent.Incoming.FORM_FINAL}] jsonObject: $jsonObject")

            val task = jsonObject.getJSONObjectOrNull("task")

            listenerInfo.formListener?.onFormFinal(
                trackId = task?.getStringOrNull("track_id"),
                taskId = task?.getLongOrNull("task_id"),
                message = jsonObject.getStringOrNull("message"),
                success = jsonObject.getBooleanOrNull("success") ?: false
            )
        }
    }

    private val onOperatorGreetListener by lazy {
        JSONObjectListener { jsonObject ->
            Logger.debug(TAG, "[${SocketEvent.Incoming.OPERATOR_GREET}] jsonObject: $jsonObject")

            listenerInfo.callListener?.onCallAgentGreet(
                Greeting(
                    callAgent = Greeting.CallAgent(
                        name = jsonObject.optString("name"),
                        fullName = jsonObject.optString("full_name"),
                        photoUrl = jsonObject.getStringOrNull("photo"),
                        audioStreamEnabled = jsonObject.getBooleanOrNull("audio_stream_enabled")
                            ?: true,
                        videoStreamEnabled = jsonObject.getBooleanOrNull("video_stream_enabled")
                            ?: true,
                    ),
                    text = jsonObject.optString("text")
                )
            )

        }
    }

    private val onOperatorTypingListener by lazy {
        Emitter.Listener {
            Logger.debug(TAG, "event [${SocketEvent.Incoming.OPERATOR_TYPING}]")
        }
    }

    private val onFeedbackListener by lazy {
        JSONObjectListener { jsonObject ->
            Logger.debug(TAG, "[${SocketEvent.Incoming.FEEDBACK}] jsonObject: $jsonObject")

            val text = jsonObject.optString("text")
            val chatId = jsonObject.optLong("chat_id", -1)

            val rateButtons = jsonObject.getJSONArrayOrNull("buttons")?.mapNotNull<JSONObject, RateButton> {
                val payload = it.getStringOrNull("payload")
                val rating = if (payload.isNullOrBlank()) {
                    null
                } else {
                    payload.split(":").getOrNull(1)?.toIntOrNull()
                }
                RateButton(
                    text = it.optString("title"),
                    chatId = chatId,
                    rating = rating ?: return@mapNotNull null
                )
            }

            listenerInfo.callListener?.onCallFeedback(
                text = text,
                rateButtons = if (rateButtons.isNullOrEmpty()) null else rateButtons
            )
        }
    }

    private val onUserQueueListener by lazy {
        JSONObjectListener { jsonObject ->
//            Logger.debug(TAG, "[USER_QUEUE] jsonObject: jsonObject")

            val count = jsonObject.getInt("count")
//            val channel = jsonObject.getInt("channel")

            listenerInfo.callListener?.onPendingUsersQueueCount(count = count)
        }
    }

    private val onMessageListener by lazy {
        JSONObjectListener { jsonObject ->
            Logger.debug(TAG, "[${SocketEvent.Incoming.MESSAGE}] jsonObject: $jsonObject")

            val id = jsonObject.getStringOrNull("id")?.trim()
            val text = jsonObject.getStringOrNull("text")?.trim()
            val action = findEnumBy<CallAction> { it.value == jsonObject.getStringOrNull("action") }
            val time = jsonObject.optLong("time")
//            val sender = jsonObject.getStringOrNull("sender")
//            val from = jsonObject.getStringOrNull("from")
            val rtcJSONObject = jsonObject.optJSONObject("rtc")
            val replyMarkupJSONObject = jsonObject.optJSONObject("reply_markup")
            val formJSONObject = jsonObject.optJSONObject("form")

            val rows = replyMarkupJSONObject?.optJSONArray("inline_keyboard")?.map<JSONArray, ReplyMarkup.Row> { rowsJSONArray ->
                val buttons = rowsJSONArray.map<JSONObject, Button> { buttonJSONObject ->
                    val buttonText = buttonJSONObject.getString("text")
                    val buttonCallbackData = buttonJSONObject.getStringOrNull("callback_data")
                    val buttonUrl = buttonJSONObject.getStringOrNull("url")
                    when {
                        !buttonCallbackData.isNullOrBlank() ->
                            CallbackButton(text = buttonText, payload = buttonCallbackData)
                        !buttonUrl.isNullOrBlank() ->
                            URLButton(text = buttonText, url = buttonUrl)
                        else ->
                            TextButton(text = buttonText)
                    }
                }

                ReplyMarkup.Row(buttons)
            }
            val replyMarkup = if (rows.isNullOrEmpty()) null else InlineReplyMarkup(rows = rows)

            if (!text.isNullOrBlank()) {
                if (jsonObject.optBoolean("no_results")) {
                    if (listenerInfo.chatBotListener?.onNoResultsFound(text, time) == true) {
                        return@JSONObjectListener
                    }
                }

                if (jsonObject.optBoolean("fuzzy_task")) {
                    if (listenerInfo.chatBotListener?.onFuzzyTaskOffered(text, time) == true) {
                        return@JSONObjectListener
                    }
                }
            }

            if (jsonObject.optBoolean("no_online")) {
                if (listenerInfo.callListener?.onNoOnlineCallAgents(text) == true) {
                    return@JSONObjectListener
                }
            }

            when (action) {
                CallAction.CHAT_TIMEOUT -> {
                    if (listenerInfo.callListener?.onLiveChatTimeout(text, time) == true) {
                        return@JSONObjectListener
                    }
                }
                CallAction.OPERATOR_DISCONNECT -> {
                    if (listenerInfo.callListener?.onCallAgentDisconnected(text, time) == true) {
                        return@JSONObjectListener
                    }
                }
                CallAction.REDIRECT -> {
                    if (listenerInfo.callListener?.onUserRedirected(text, time) == true) {
                        return@JSONObjectListener
                    }
                }
                else -> {
                }
            }

            if (rtcJSONObject != null) {
                when (findEnumBy<QRTCAction> { it.value == rtcJSONObject.getStringOrNull("type") }) {
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
                        val sdp = rtcJSONObject.getStringOrNull("sdp")

                        if (sdp.isNullOrBlank()) {
                            Logger.error(TAG, "Session description is null or blank!")
                            return@JSONObjectListener
                        } else {
                            listenerInfo.webRTCListener?.onCallOffer(
                                SessionDescription(
                                    type = SessionDescription.Type.OFFER,
                                    description = sdp
                                )
                            )
                        }
                    }
                    QRTCAction.ANSWER -> {
                        val sdp = rtcJSONObject.getStringOrNull("sdp")

                        if (sdp.isNullOrBlank()) {
                            Logger.error(TAG, "Session description is null or blank!")
                            return@JSONObjectListener
                        } else {
                            listenerInfo.webRTCListener?.onCallAnswer(
                                SessionDescription(
                                    type = SessionDescription.Type.ANSWER,
                                    description = sdp
                                )
                            )
                        }
                    }
                    QRTCAction.CANDIDATE ->
                        listenerInfo.webRTCListener?.onRemoteIceCandidate(
                            IceCandidate(
                                sdpMid = rtcJSONObject.getString("id"),
                                sdpMLineIndex = rtcJSONObject.getInt("label"),
                                sdp = rtcJSONObject.getString("candidate")
                            )
                        )
                    QRTCAction.HANGUP ->
                        listenerInfo.webRTCListener?.onPeerHangupCall()
                    else -> {
                        Logger.error(TAG, "Unsupported type for: $rtcJSONObject")
                    }
                }
                return@JSONObjectListener
            }

            if (!jsonObject.isNull("queued")) {
                val queued = jsonObject.optInt("queued")
                listenerInfo.callListener?.onPendingUsersQueueCount(text, queued)
            }

            val attachments = jsonObject.getJSONArrayOrNull("attachments")?.mapNotNull<JSONObject, Content> {
                val hash = it.getStringOrNull("hash")
                val title = it.getStringOrNull("title")
                val url = it.getStringOrNull("url") ?: return@mapNotNull null
                when (ContentType.from(it.getStringOrNull("type")) ?: return@mapNotNull null) {
                    ContentType.IMAGE -> {
                        Image(
                            id = hash,
                            title = title,
                            remoteFile = Content.RemoteFile(url)
                        )
                    }
                    ContentType.VIDEO -> {
                        Video(
                            id = hash,
                            title = title,
                            remoteFile = Content.RemoteFile(url)
                        )
                    }
                    ContentType.AUDIO -> {
                        Audio(
                            id = hash,
                            title = title,
                            remoteFile = Content.RemoteFile(url)
                        )
                    }
                    ContentType.DOCUMENT -> {
                        Document(
                            id = hash,
                            title = title,
                            remoteFile = Content.RemoteFile(url)
                        )
                    }
                    else -> {
                        Logger.error(TAG, "Unsupported type of message attachment")
                        null
                    }
                }
            }

            val message = Message.Builder()
                .setId(id ?: Entity.generateId())
                .setIncomingDirection()
                .setCreatedAt(time)
                .setBody(text)
                .setReplyMarkup(replyMarkup)
                .setContents(
                    listOfNotNull(parseContent(jsonObject.getJSONObjectOrNull("media"))) + (attachments ?: emptyList()))
                .build()

            if (formJSONObject?.has("id") == true) {
                val form = Form(
                    id = formJSONObject.optLong("id"),
                    title = formJSONObject.getStringOrNull("title") ?: "",
                    prompt = formJSONObject.getStringOrNull("prompt"),
                    fields = emptyList()
                )
                if (listenerInfo.formListener?.onFormFound(message, form) == true) {
                    return@JSONObjectListener
                }
            }

            listenerInfo.coreListener?.onMessage(message = message)
        }
    }

    private val onCategoryListListener by lazy {
        JSONObjectListener { jsonObject ->
            Logger.debug(TAG, "[${SocketEvent.Incoming.CATEGORY_LIST}] jsonObject: $jsonObject")

            fun parse(jsonObject: JSONObject): Category? {
                val responses = jsonObject.getJSONArrayOrNull("responses")
                    ?.mapNotNull<Any, Long> { responseId ->
                        when (responseId) {
                            is Int -> responseId.toLong()
                            is Long -> responseId
                            else -> null
                        }
                    } ?: emptyList()
                return Category(
                    id = jsonObject.getLongOrNull("id") ?: return null,
                    title = jsonObject.getStringOrNull("title")?.trim(),
                    language = findEnumBy { it.value == jsonObject.getLongOrNull("lang") } ?: Language.ID.RU,
                    parentId = jsonObject.getLongOrNull("parent_id") ?: Category.NO_PARENT_ID,
                    photo = jsonObject.getStringOrNull("photo"),
                    responses = responses,
                    config = Category.Config(
                        jsonObject.getJSONObjectOrNull("config")?.getIntOrNull("order") ?: 0
                    )
                )
            }

            val categories = jsonObject.optJSONArray("category_list")?.mapNotNull<JSONObject, Category> { categoryJSONObject ->
                parse(categoryJSONObject)
            }

            listenerInfo.chatBotListener?.onCategories(categories?.sortedBy { it.config?.order } ?: emptyList())
        }
    }

    private val onCard102UpdateListener by lazy {
        JSONObjectListener { jsonObject ->
            Logger.debug(TAG, "[${SocketEvent.Incoming.CARD102_UPDATE}] jsonObject: $jsonObject")

            val status = Card102Status(jsonObject.getIntOrNull("status") ?: -1)

            if (status == null) {
                Logger.debug(TAG, "Incorrect/unsupported Card102 status: $status")
            } else {
                listenerInfo.armListener?.onCard102Update(status)
            }
        }
    }

    private val onLocationUpdate by lazy {
        JSONArrayListener { jsonArray ->
            // [{"Gps_Code":7170891,"X":71.4771061686837,"Y":51.1861201686837},{"Gps_Code":7171196,"X":71.43119816868371,"Y":51.1138291686837},{"Gps_Code":7170982,"X":71.5110101686837,"Y":51.1387631686837}]

            Logger.debug(TAG, "[${SocketEvent.Incoming.LOCATION_UPDATE}] jsonArray: $jsonArray")

            val locationUpdates = jsonArray.map<JSONObject, LocationUpdate> {
                val gpsCode = it.optLong("Gps_Code", -1)
                val x = it.optDouble("X", -1.0)
                val y = it.optDouble("Y", -1.0)

                LocationUpdate(gpsCode, x, y)
            }

            listenerInfo.armListener?.onLocationUpdate(locationUpdates)
        }
    }

    private val onTaskMessageListener by lazy {
        JSONObjectListener { jsonObject ->
            Logger.debug(TAG, "[${SocketEvent.Incoming.TASK_MESSAGE}] jsonObject: $jsonObject")

            val notificationJSONObject = jsonObject.getJSONObjectOrNull("notification")
            val taskJSONObject = jsonObject.getJSONObjectOrNull("task") ?: return@JSONObjectListener
            val id = jsonObject.getLongOrNull("id")
            val text = jsonObject.getStringOrNull("text")
            val mediaJSONObject = jsonObject.getJSONObjectOrNull("media")

            listenerInfo.taskListener?.onTaskMessage(
                TaskMessage(
                    id = id ?: -1,
                    notification = TaskMessage.Notification(
                        title = notificationJSONObject?.getStringOrNull("title"),
                        url = notificationJSONObject?.getStringOrNull("url")
                    ),
                    task = TaskMessage.Task(
                        id = taskJSONObject.getLong("id"),
                        trackId = taskJSONObject.getStringOrNull("track_id")
                    ),
                    text = text,
                    contents = listOfNotNull(parseContent(mediaJSONObject))
                )
            )
        }
    }

    private val onDisconnectListener by lazy {
        Emitter.Listener {
            Logger.debug(TAG, "event [${Socket.EVENT_DISCONNECT}]")

            listenerInfo.socketStateListener?.onSocketDisconnect()
        }
    }

    private fun parseContent(jsonObject: JSONObject?): Content? {
        if (jsonObject == null) return null

        val image = jsonObject.getStringOrNull("image")
        val audio = jsonObject.getStringOrNull("audio")
        val video = jsonObject.getStringOrNull("video")
        val document = jsonObject.getStringOrNull("document")
        val file = jsonObject.getStringOrNull("file")

        val hash = jsonObject.getStringOrNull("hash")
        val name = jsonObject.getStringOrNull("name")
        val ext = jsonObject.getStringOrNull("ext")

        val extension = findEnumBy<Extension> { it.value == ext }

        return if (ext.isNullOrBlank()) {
            null
        } else {
            when {
                !image.isNullOrBlank() -> {
                    Image(
                        id = hash,
                        title = name,
                        remoteFile = Content.RemoteFile(image)
                    )
                }
                !video.isNullOrBlank() -> {
                    Video(
                        id = hash,
                        title = name,
                        remoteFile = Content.RemoteFile(video)
                    )
                }
                !audio.isNullOrBlank() -> {
                    Audio(
                        id = hash,
                        title = name,
                        remoteFile = Content.RemoteFile(audio)
                    )
                }
                !document.isNullOrBlank() -> {
                    Document(
                        id = hash,
                        title = name,
                        remoteFile = Content.RemoteFile(document)
                    )
                }
                !file.isNullOrBlank() -> {
                    when (extension) {
                        in Extensions.IMAGE.values -> {
                            Image(
                                id = hash,
                                title = name,
                                remoteFile = Content.RemoteFile(file)
                            )
                        }
                        in Extensions.VIDEO.values -> {
                            Video(
                                id = hash,
                                title = name,
                                remoteFile = Content.RemoteFile(file)
                            )
                        }
                        in Extensions.AUDIO.values -> {
                            Audio(
                                id = hash,
                                title = name,
                                remoteFile = Content.RemoteFile(file)
                            )
                        }
                        in Extensions.DOCUMENT.values -> {
                            Document(
                                id = hash,
                                title = name,
                                remoteFile = Content.RemoteFile(file)
                            )
                        }
                        else -> {
                            null
                        }
                    }
                }
                else -> {
                    null
                }
            }
        }
    }

}