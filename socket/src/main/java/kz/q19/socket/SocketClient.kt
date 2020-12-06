@file:Suppress("unused")

package kz.q19.socket

import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kz.q19.domain.model.*
import kz.q19.domain.model.webrtc.*
import kz.q19.socket.event.SocketEvent
import kz.q19.socket.listener.*
import kz.q19.socket.repository.SocketRepository
import kz.q19.socket.utils.Logger
import kz.q19.utils.enums.findEnumBy
import kz.q19.utils.json.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class SocketClient private constructor() : SocketRepository {

    companion object {
        private const val TAG = "SocketRepositoryImpl"

        @Volatile
        private var INSTANCE: SocketClient? = null

        fun getInstance(): SocketClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SocketClient().also { INSTANCE = it }
            }
        }
    }

    var socket: Socket? = null

    private val language: String
        get() {
            val language = SocketClientConfig.getLanguage()
            return language.key
        }

    private var lastActiveTime: Long = -1L

    private val listenerInfo: ListenerInfo by lazy { ListenerInfo() }

    override fun setSocketStateListener(socketStateListener: SocketStateListener?) {
        Logger.debug(TAG, "setSocketStateListener() -> socketStateListener: $socketStateListener")

        listenerInfo.socketStateListener = socketStateListener
    }

    override fun setBasicListener(basicListener: BasicListener?) {
        Logger.debug(TAG, "setBasicListener() -> basicListener: $basicListener")

        listenerInfo.basicListener = basicListener
    }

    override fun setWebRTCListener(webRTCListener: WebRTCListener?) {
        Logger.debug(TAG, "setWebRTCListener() -> webRTCListener: $webRTCListener")

        listenerInfo.webRTCListener = webRTCListener
    }

    override fun setDialogListener(dialogListener: DialogListener?) {
        Logger.debug(TAG, "setDialogListener() -> dialogListener: $dialogListener")

        listenerInfo.dialogListener = dialogListener
    }

    override fun setFormListener(formListener: FormListener?) {
        Logger.debug(TAG, "setFormListener() -> formListener: $formListener")

        listenerInfo.formListener = formListener
    }

    override fun setLocationListener(locationListener: LocationListener?) {
        Logger.debug(TAG, "setLocationListener() -> locationListener: $locationListener")

        listenerInfo.locationListener = locationListener
    }

    override fun removeAllListeners() {
        Logger.debug(TAG, "removeAllListeners()")

        listenerInfo.clear()
    }

    override fun connect(url: String) {
        Logger.debug(TAG, "connect() -> url: $url")

        val options = IO.Options()
        options.reconnection = true
        options.reconnectionAttempts = 3

        socket = IO.socket(url, options)

        socket?.on(Socket.EVENT_CONNECT, onConnectListener)
//        socket?.on(SocketEvent.Incoming.CALL, onCallListener)
        socket?.on(SocketEvent.Incoming.OPERATOR_GREET, onOperatorGreetListener)
        socket?.on(SocketEvent.Incoming.FORM_INIT, onFormInitListener)
        socket?.on(SocketEvent.Incoming.FORM_FINAL, onFormFinalListener)
        socket?.on(SocketEvent.Incoming.FEEDBACK, onFeedbackListener)
        socket?.on(SocketEvent.Incoming.USER_QUEUE, onUserQueueListener)
        socket?.on(SocketEvent.Incoming.OPERATOR_TYPING, onOperatorTypingListener)
        socket?.on(SocketEvent.Incoming.MESSAGE, onMessageListener)
        socket?.on(SocketEvent.Incoming.CATEGORY_LIST, onCategoryListListener)
        socket?.on(Socket.EVENT_DISCONNECT, onDisconnectListener)

        socket?.connect()
    }

    override fun release() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    override fun getLastActiveTime(): Long {
        return lastActiveTime
    }

    override fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }

    override fun initializeCall(callType: CallType, language: Language, scope: String?) {
        Logger.debug(TAG, "initializeCall() -> callType: $callType, language: $language, scope: $scope")

        when (callType) {
            CallType.TEXT -> {
                emit(SocketEvent.Outgoing.INITIALIZE, json {
                    put("video", false)
                    putIfValueNotNull("scope", scope)
                    put("lang", language)
                })
            }
            CallType.AUDIO -> {
                emit(SocketEvent.Outgoing.INITIALIZE, json {
                    put("audio", true)
                    putIfValueNotNull("scope", scope)
                    put("lang", language)
                })
            }
            CallType.VIDEO -> {
                emit(SocketEvent.Outgoing.INITIALIZE, json {
                    put("video", true)
                    putIfValueNotNull("scope", scope)
                    put("lang", language)
                })
            }
        }
    }

    override fun initializeCall(
        callType: CallType,
        userId: Long,
        domain: String?,
        topic: String?,
        location: Location?,
        language: Language
    ) {
        Logger.debug(TAG, "initializeCall() -> $callType, $userId, $domain, $topic, $location, $language")

        when (callType) {
            CallType.TEXT -> {
                emit(SocketEvent.Outgoing.INITIALIZE, json {
                    put("user_id", userId)
                    putIfValueNotNull("domain", domain)
                    putIfValueNotNull("topic", topic)

                    if (location != null) {
                        put("location", json {
                            put("lat", location.latitude)
                            put("lon", location.longitude)
                        })
                    }

                    put("lang", language.key)
                })
            }
            CallType.AUDIO -> {
                emit(SocketEvent.Outgoing.INITIALIZE, json {
                    put("user_id", userId)
                    put("media", "audio")
                    putIfValueNotNull("domain", domain)
                    putIfValueNotNull("topic", topic)

                    if (location != null) {
                        put("location", json {
                            put("lat", location.latitude)
                            put("lon", location.longitude)
                        })
                    }

                    put("lang", language.key)
                })
            }
            CallType.VIDEO -> {
                emit(SocketEvent.Outgoing.INITIALIZE, json {
                    put("user_id", userId)
                    put("media", "video")
                    putIfValueNotNull("domain", domain)
                    putIfValueNotNull("topic", topic)

                    if (location != null) {
                        put("location", json {
                            put("lat", location.latitude)
                            put("lon", location.longitude)
                        })
                    }

                    put("lang", language.key)
                })
            }
        }
    }

    override fun getParentCategories() {
        getCategories(parentId = Category.NO_PARENT_ID)
    }

    override fun getCategories(parentId: Long) {
        emit(SocketEvent.Outgoing.USER_DASHBOARD, json {
            put("action", "get_category_list")
            put("parent_id", parentId)
            put("lang", language)
        })
    }

    override fun getResponse(id: Long) {
        emit(SocketEvent.Outgoing.USER_DASHBOARD, json {
            put("action", "get_response")
            put("id", id)
            put("lang", language)
        })
    }

    override fun sendUserLanguage(language: Language) {
        Logger.debug(TAG, "sendUserLanguage() -> language: $language")

        emit(SocketEvent.Outgoing.USER_LANGUAGE, json {
            put("language", language.key)
        })
    }

    override fun sendUserMessage(message: String) {
        Logger.debug(TAG, "sendUserMessage() -> message: $message")

        if (message.isBlank()) {
            return
        }

        val text = message.trim()

        emit(SocketEvent.Outgoing.USER_MESSAGE, json {
            put("text", text)
            put("lang", language)
        })
    }

    override fun sendUserMediaMessage(type: Media.Type, url: String) {
        emit(SocketEvent.Outgoing.USER_MESSAGE, json {
            put(type.key, url)
        })
    }

    override fun sendUserFeedback(rating: Int, chatId: Long) {
        emit(SocketEvent.Outgoing.USER_FEEDBACK, json {
            put("r", rating)
            put("chat_id", chatId)
        })
    }

    override fun sendUserLocation(id: String, location: Location) {
        Logger.debug(TAG, "sendUserLocation() -> location: $location")

        emit(SocketEvent.Outgoing.USER_LOCATION, json {
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
        })
    }

    override fun sendLocationSubscribe() {
        Logger.debug(TAG, "sendLocationSubscribe()")

        socket?.on(SocketEvent.Incoming.LOCATION_UPDATE, onLocationUpdate)

        emit(SocketEvent.Outgoing.LOCATION_SUBSCRIBE)
    }

    override fun sendLocationUnsubscribe() {
        Logger.debug(TAG, "sendLocationUnsubscribe()")

        socket?.off(SocketEvent.Incoming.LOCATION_UPDATE, onLocationUpdate)
    }

    override fun sendMessage(webRTCInfo: WebRTCInfo?, action: Message.Action?) {
        Logger.debug(TAG, "sendMessage() -> webRTC: $webRTCInfo, action: $action")

        val messageObject = JSONObject()

        try {
            if (webRTCInfo != null) {
                messageObject.put("rtc", json {
                    put("type", webRTCInfo.type.value)
                    putIfValueNotNull("id", webRTCInfo.id)
                    putIfValueNotNull("label", webRTCInfo.label)
                    putIfValueNotNull("candidate", webRTCInfo.candidate)
                    putIfValueNotNull("sdp", webRTCInfo.sdp)
                })
            }

            if (action != null) {
                messageObject.put("action", action.value)
            }

            messageObject.put("lang", language)
        } catch (e: JSONException) {
            e.printStackTrace()
            Logger.error(TAG, e)
        }

        Logger.debug(TAG, "sendMessage() -> messageObject: $messageObject")

        emit(SocketEvent.Outgoing.MESSAGE, messageObject)
    }

    override fun sendMessage(id: String, location: Location) {
        Logger.debug(TAG, "sendMessage() -> id: $id, location: $location")

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
        })
    }

    override fun sendFuzzyTaskConfirmation(name: String, email: String, phone: String) {
        emit(SocketEvent.Outgoing.CONFIRM_FUZZY_TASK, json {
            put("name", name)
            put("email", email)
            put("phone", phone)
            put("res", '1')
        })
    }

    override fun sendExternal(callbackData: String?) {
        emit(SocketEvent.Outgoing.EXTERNAL, json {
            put("callback_data", callbackData)
        })
    }

    override fun sendFormInitialize(formId: Long) {
        emit(SocketEvent.Outgoing.FORM_INIT, json {
            put("form_id", formId)
        })
    }

    override fun sendFormFinalize(form: Form, sender: String?, extraFields: List<JSONObject>?) {
        emit(SocketEvent.Outgoing.FORM_FINAL, json {
            putIfValueNotNull("sender", sender)

            put("form_id", form.id)

            val nodes = JSONArray()
            val fields = JSONObject()

            extraFields?.forEach {
                fields.put(
                    it.getString("title"),
                    json { put(it.getString("type"), it.getString("value")) }
                )
            }

            form.fields.forEach { field ->
                if (field.isFlex) {
                    nodes.put(json {
                        put(field.type.value, field.value)
                        put(
                            "${field.type.value}_info",
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
                    if (!title.isNullOrBlank()) {
                        fields.put(title, json { put(field.type.value, field.value) })
                    }
                }
            }

            put("form_data", json {
                put("nodes", nodes)
                put("fields", fields)
            })
        })
    }

    override fun sendCancel() {
        emit(SocketEvent.Outgoing.CANCEL)
    }

    override fun sendCancelPendingCall() {
        emit(SocketEvent.Outgoing.CANCEL_PENDING_CALL)
    }

    private fun emit(event: String, jsonObject: JSONObject? = null): Emitter? {
        return socket?.emit(
            event,
            jsonObject,
            Ack { args ->
                Logger.debug(TAG, "args: $args")
            }
        )
    }
    
    private val onConnectListener = Emitter.Listener {
        Logger.debug(TAG, "event [${Socket.EVENT_CONNECT}]")

        listenerInfo.socketStateListener?.onConnect()
    }

    private val onOperatorGreetListener = Emitter.Listener { args ->
        Logger.debug(TAG, "event [${SocketEvent.Incoming.OPERATOR_GREET}]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        Logger.debug(TAG, "[${SocketEvent.Incoming.OPERATOR_GREET}] data: $data")

//        val name = data.optString("name")
        val fullName = data.optString("full_name")

        // Url path
        val photo = data.optString("photo")

        val text = data.optString("text")

        Logger.debug(TAG, "listenerInfo.dialogListener: ${listenerInfo.dialogListener}")

        listenerInfo.dialogListener?.onOperatorGreet(fullName, photo, text)
    }

    private val onFormInitListener = Emitter.Listener { args ->
        Logger.debug(TAG, "event [${SocketEvent.Incoming.FORM_INIT}]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        Logger.debug(TAG, "[FORM_INIT] data: $data")

        val formJson = data.getJSONObject("form")
        val formFieldsJsonArray = data.getJSONArray("form_fields")

        val fields = mutableListOf<Form.Field>()
        for (i in 0 until formFieldsJsonArray.length()) {
            val formFieldJson = formFieldsJsonArray[i] as JSONObject
            fields.add(
                Form.Field(
                    id = formFieldJson.getLong("id"),
                    title = formFieldJson.getStringOrNull("title"),
                    prompt = formFieldJson.getStringOrNull("prompt"),
                    type = findEnumBy { it.value == formFieldJson.getString("type") } ?: Form.Field.Type.TEXT,
                    default = formFieldJson.getStringOrNull("default"),
                    formId = formFieldJson.getLong("form_id"),
                    configs = null,
                    level = formFieldJson.optInt("level", -1),
                    value = null
                )
            )
        }

        val form = Form(
            id = formJson.getLong("id"),
            title = formJson.getStringOrNull("title"),
            isFlex = formJson.optInt("is_flex"),
            prompt = formJson.getStringOrNull("prompt"),
            fields = fields
        )

        Logger.debug(TAG, "listenerInfo.formListener: ${listenerInfo.formListener}")

        listenerInfo.formListener?.onFormInit(form)
    }

    private val onFormFinalListener = Emitter.Listener { args ->
        Logger.debug(TAG, "event [${SocketEvent.Incoming.FORM_FINAL}]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        Logger.debug(TAG, "[FORM_FINAL] data: $data")

        val taskJson = data.optJSONObject("task")
        val trackId = taskJson?.getStringOrNull("track_id")
//        val message = data.getStringOrNull("message")
//        val success = data.optBoolean("success", false)

        listenerInfo.formListener?.onFormFinal(text = trackId ?: "")
    }

    private val onOperatorTypingListener = Emitter.Listener {
        Logger.debug(TAG, "event [${SocketEvent.Incoming.OPERATOR_TYPING}]")
    }

    private val onFeedbackListener = Emitter.Listener { args ->
        Logger.debug(TAG, "event [${SocketEvent.Incoming.FEEDBACK}]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        Logger.debug(TAG, "[FEEDBACK] data: $data")

        val buttonsJson = data.optJSONArray("buttons")

        val text = data.optString("text")
//        val chatId = feedback.optLong("chat_id")

        if (buttonsJson != null) {
            val ratingButtons = mutableListOf<RatingButton>()
            for (i in 0 until buttonsJson.length()) {
                val button = buttonsJson[i] as JSONObject
                ratingButtons.add(
                    RatingButton(
                        button.optString("title"),
                        button.optString("payload")
                    )
                )
            }

            listenerInfo.dialogListener?.onFeedback(text, ratingButtons)
        }
    }

    private val onUserQueueListener = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [USER_QUEUE]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        Logger.debug(TAG, "[USER_QUEUE] data: $data")

        val count = data.getInt("count")
//            val channel = userQueue.getInt("channel")

        listenerInfo.basicListener?.onPendingUsersQueueCount(count = count)
    }

    private val onMessageListener = Emitter.Listener { args ->
        Logger.debug(TAG, "event [${SocketEvent.Incoming.MESSAGE}]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        Logger.debug(TAG, "[MESSAGE] data: $data")

        val id = data.getStringOrNull("id")?.trim()
        val text = data.getStringOrNull("text")?.trim()
        val noOnline = data.optBoolean("no_online")
        val noResults = data.optBoolean("no_results")
//        val id = message.optString("id")
        val action = findEnumBy<Message.Action> { it.value == data.getStringOrNull("action") }
        val time = data.optLong("time")
        val sender = data.getStringOrNull("sender")
        val from = data.getStringOrNull("from")
        val mediaJsonObject = data.optJSONObject("media")
        val rtcJsonObject = data.optJSONObject("rtc")
        val fuzzyTask = data.optBoolean("fuzzy_task")
//        val form = message.optJSONObject("form")
        val attachmentsJsonArray = data.optJSONArray("attachments")
        val replyMarkupJsonObject = data.optJSONObject("reply_markup")
        val formJsonObject = data.optJSONObject("form")

//        Logger.debug(TAG, "replyMarkupJsonObject: $replyMarkupJsonObject")
//        Logger.debug(TAG, "action: $action")

        Logger.debug(TAG, "listenerInfo.basicListener: ${listenerInfo.basicListener}")

        var replyMarkup: Message.ReplyMarkup? = null
        if (replyMarkupJsonObject != null) {
            val rows = mutableListOf<List<Message.ReplyMarkup.Button>>()

            val inlineKeyboard = replyMarkupJsonObject.optJSONArray("inline_keyboard")
            Logger.debug(TAG, "inlineKeyboard: $inlineKeyboard")
            if (inlineKeyboard != null) {
                for (i in 0 until inlineKeyboard.length()) {
                    val row = inlineKeyboard[i] as? JSONArray?

                    Logger.debug(TAG, "row: $row")

                    val buttons = mutableListOf<Message.ReplyMarkup.Button>()
                    for (j in 0 until (row?.length() ?: 0)) {
                        val button = row?.get(j) as? JSONObject?
                        Logger.debug(TAG, "button: $button")

                        buttons.add(
                            Message.ReplyMarkup.Button(
                                text = button?.getString("text") ?: "",
                                callbackData = button?.getStringOrNull("callback_data"),
                                url = button?.getStringOrNull("url")
                            )
                        )
                    }
                    rows.add(buttons)
                }
            }

            replyMarkup = Message.ReplyMarkup(rows)
        }

        var form: Form? = null
        if (formJsonObject != null && formJsonObject.has("id")) {
            form = Form(
                id = formJsonObject.optLong("id"),
                title = formJsonObject.getStringOrNull("title"),
                prompt = formJsonObject.getStringOrNull("prompt")
            )
        }

        if (noResults && from.isNullOrBlank() && sender.isNullOrBlank() && action == null && !text.isNullOrBlank()) {
            val isHandled = listenerInfo.basicListener?.onNoResultsFound(text, time)
            if (isHandled == true) return@Listener
        }

        if (fuzzyTask && !text.isNullOrBlank()) {
            val isHandled = listenerInfo.basicListener?.onFuzzyTaskOffered(text, time)
            if (isHandled == true) return@Listener
        }

        if (noOnline && !text.isNullOrBlank()) {
            val isHandled = listenerInfo.basicListener?.onNoOnlineOperators(text)
            if (isHandled == true) return@Listener
        }

        if (action == Message.Action.CHAT_TIMEOUT && !text.isNullOrBlank()) {
            val isHandled = listenerInfo.dialogListener?.onChatTimeout(text, time)
            if (isHandled == true) return@Listener
        }

        if (action == Message.Action.OPERATOR_DISCONNECT && !text.isNullOrBlank()) {
            val isHandled = listenerInfo.dialogListener?.onOperatorDisconnected(text, time)
            if (isHandled == true) return@Listener
        }

        if (action == Message.Action.REDIRECT && !text.isNullOrBlank()) {
            val isHandled = listenerInfo.dialogListener?.onUserRedirected(text, time)
            if (isHandled == true) return@Listener
        }

        if (rtcJsonObject != null) {
            when (rtcJsonObject.getStringOrNull("type")) {
                WebRTCInfo.Type.START?.value -> {
                    when (action) {
                        Message.Action.CALL_ACCEPT ->
                            listenerInfo.webRTCListener?.onCallAccept()
                        Message.Action.CALL_REDIRECT ->
                            listenerInfo.webRTCListener?.onCallRedirect()
                        Message.Action.CALL_REDIAL -> {
                        }
                        else -> {
                        }
                    }
                }
                WebRTCInfo.Type.PREPARE?.value ->
                    listenerInfo.webRTCListener?.onCallPrepare()
                WebRTCInfo.Type.READY?.value ->
                    listenerInfo.webRTCListener?.onCallReady()
                WebRTCInfo.Type.OFFER?.value -> {
                    val type = WebRTCInfo.Type.by(rtcJsonObject.getString("type"))
                    val sdp = rtcJsonObject.getString("sdp")

                    if (type != null) {
                        listenerInfo.webRTCListener?.onCallOffer(SessionDescription(type, sdp))
                    }
                }
                WebRTCInfo.Type.ANSWER?.value -> {
                    val type = WebRTCInfo.Type.by(rtcJsonObject.getString("type"))
                    val sdp = rtcJsonObject.getString("sdp")

                    if (type != null) {
                        listenerInfo.webRTCListener?.onCallAnswer(SessionDescription(type, sdp))
                    }
                }
                WebRTCInfo.Type.CANDIDATE?.value ->
                    listenerInfo.webRTCListener?.onIceCandidate(
                        IceCandidate(
                            sdpMid = rtcJsonObject.getString("id"),
                            sdpMLineIndex = rtcJsonObject.getInt("label"),
                            sdp = rtcJsonObject.getString("candidate")
                        )
                    )
                WebRTCInfo.Type.HANGUP?.value ->
                    listenerInfo.webRTCListener?.onHangup()
            }
            return@Listener
        }

        if (!data.isNull("queued")) {
            val queued = data.optInt("queued")
            listenerInfo.basicListener?.onPendingUsersQueueCount(text, queued)
        }

        val attachments = mutableListOf<Media>()
        if (attachmentsJsonArray != null) {
            for (i in 0 until attachmentsJsonArray.length()) {
                val attachment = attachmentsJsonArray[i] as? JSONObject?
                attachments.add(
                    Media(
                        id = -1,
                        title = attachment?.getStringOrNull("title"),
                        extension = findEnumBy { it.value == attachment?.getStringOrNull("ext") },
                        type = findEnumBy { it.key == attachment?.getStringOrNull("type") },
                        urlPath = attachment?.getStringOrNull("url")
                    )
                )
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

        listenerInfo.basicListener?.onMessage(
            message = Message(
                id = id,
                type = Message.Type.INCOMING,
                text = text,
                replyMarkup = replyMarkup,
                media = media,
                attachments = attachments,
                form = form,
                timestamp = time
            )
        )
    }

    private val onCategoryListListener = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [${SocketEvent.Incoming.CATEGORY_LIST}]")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        val categoryListJson = data.optJSONArray("category_list") ?: return@Listener

//        Logger.debug(TAG, "categoryList: $data")

        fun parse(jsonObject: JSONObject): Category {
            return Category(
                id = jsonObject.optLong("id"),
                title = jsonObject.optString("title").trim(),
                language = findEnumBy { it.value == jsonObject.optLong("lang") } ?: Language.ID.RU,
                parentId = jsonObject.getLongOrNull("parent_id") ?: Category.NO_PARENT_ID,
                photo = jsonObject.optString("photo"),
                responses = jsonObject.getAsMutableList("responses"),
                config = Category.Config(jsonObject.optJSONObject("config")?.optInt("order") ?: 0)
            )
        }

        val currentCategories = mutableListOf<Category>()
        for (i in 0 until categoryListJson.length()) {
            (categoryListJson[i] as? JSONObject?)?.let { categoryJson ->
//                Logger.debug(TAG, "categoryJson: $categoryJson")
                val parsed = parse(categoryJson)
                currentCategories.add(parsed)
            }
        }

        listenerInfo.basicListener?.onCategories(currentCategories.sortedBy { it.config?.order })
    }

    private val onLocationUpdate = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [${SocketEvent.Incoming.LOCATION_UPDATE}]")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        Logger.debug(TAG, "[${SocketEvent.Incoming.LOCATION_UPDATE}] data: $data")

        val coordsJsonArray = data.optJSONArray("coords") ?: return@Listener

        if (coordsJsonArray.length() == 2) {
            val longitude = coordsJsonArray.getDouble(0)
            val latitude = coordsJsonArray.getDouble(1)
            listenerInfo.locationListener?.onLocationUpdate(
                Location(longitude = longitude, latitude = latitude)
            )
        }
    }

    private val onDisconnectListener = Emitter.Listener {
        Logger.debug(TAG, "event [${Socket.EVENT_DISCONNECT}]")

        lastActiveTime = System.currentTimeMillis()

        listenerInfo.socketStateListener?.onDisconnect()
    }

}