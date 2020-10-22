@file:Suppress("unused")

package kz.q19.socket

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kz.q19.common.preferences.PreferencesProvider
import kz.q19.domain.model.*
import kz.q19.domain.model.webrtc.WebRTC
import kz.q19.domain.model.webrtc.WebRTCIceCandidate
import kz.q19.domain.model.webrtc.WebRTCSessionDescription
import kz.q19.socket.model.UserLocation
import kz.q19.utils.enums.findEnumBy
import kz.q19.utils.json.getAsMutableList
import kz.q19.utils.json.getLongOrNull
import kz.q19.utils.json.getStringOrNull
import kz.q19.utils.json.json
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class SocketClient private constructor(
    private val preferencesProvider: PreferencesProvider
) : SocketRepository {

    companion object {
        private const val TAG = "SocketRepositoryImpl"

        @Volatile
        private var INSTANCE: SocketClient? = null

        fun getInstance(preferencesProvider: PreferencesProvider): SocketClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SocketClient(preferencesProvider).also { INSTANCE = it }
            }
        }
    }

    private var socket: Socket? = null

    private val language: String
        get() {
            var language = preferencesProvider.getLanguage()
            if (language.isBlank()) {
                language = Language.DEFAULT.key
            }
            return language
        }

    private var lastActiveTime: Long = -1L

    private var socketStateListener: SocketStateListener? = null
    private var generalListener: GeneralListener? = null
    private var webRTCListener: WebRTCListener? = null

    override fun setSocketStateListener(socketStateListener: SocketStateListener?) {
        this.socketStateListener = socketStateListener
    }

    override fun setGeneralListener(generalListener: GeneralListener?) {
        this.generalListener = generalListener
    }

    override fun setWebRTCListener(webRTCListener: WebRTCListener?) {
        this.webRTCListener = webRTCListener
    }

    override fun removeListeners() {
        this.socketStateListener = null
        this.generalListener = null
        this.webRTCListener = null
    }

    override fun connect(url: String) {
        val options = IO.Options()
        options.reconnection = true
        options.reconnectionAttempts = 3

        socket = IO.socket(url, options)

        socket?.on(Socket.EVENT_CONNECT, onConnectListener)
//        socket?.on(IncomingSocketEvent.CALL, onCallListener)
        socket?.on(IncomingSocketEvent.OPERATOR_GREET, onOperatorGreetListener)
        socket?.on(IncomingSocketEvent.FORM_INIT, onFormInitListener)
        socket?.on(IncomingSocketEvent.FORM_FINAL, onFormFinalListener)
        socket?.on(IncomingSocketEvent.FEEDBACK, onFeedbackListener)
        socket?.on(IncomingSocketEvent.USER_QUEUE, onUserQueueListener)
        socket?.on(IncomingSocketEvent.OPERATOR_TYPING, onOperatorTypingListener)
        socket?.on(IncomingSocketEvent.MESSAGE, onMessageListener)
        socket?.on(IncomingSocketEvent.CATEGORY_LIST, onCategoryListListener)
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
                emit(
                    OutgoingSocketEvent.INITIALIZE_CALL,
                    json {
                        put("video", false)
                        if (!scope.isNullOrBlank()) {
                            put("scope", scope)
                        }
                        put("lang", language)
                    }
                )
            }
            CallType.AUDIO -> {
                emit(
                    OutgoingSocketEvent.INITIALIZE_CALL,
                    json {
                        put("audio", true)
                        if (!scope.isNullOrBlank()) {
                            put("scope", scope)
                        }
                        put("lang", language)
                    }
                )
            }
            CallType.VIDEO -> {
                emit(
                    OutgoingSocketEvent.INITIALIZE_CALL,
                    json {
                        put("video", true)
                        if (!scope.isNullOrBlank()) {
                            put("scope", scope)
                        }
                        put("lang", language)
                    }
                )
            }
        }
    }

    override fun getParentCategories() {
        getCategories(parentId = Category.NO_PARENT_ID)
    }

    override fun getCategories(parentId: Long) {
        emit(
            OutgoingSocketEvent.USER_DASHBOARD,
            json {
                put("action", "get_category_list")
                put("parent_id", parentId)
                put("lang", language)
            }
        )
    }

    override fun getResponse(id: Long) {
        emit(
            OutgoingSocketEvent.USER_DASHBOARD,
            json {
                put("action", "get_response")
                put("id", id)
                put("lang", language)
            }
        )
    }

    override fun sendUserLanguage(language: Language) {
        Logger.debug(TAG, "sendUserLanguage() -> language: $language")

        emit(
            OutgoingSocketEvent.USER_LANGUAGE,
            json {
                put("language", language.key)
            }
        )
    }

    override fun sendUserMessage(message: String) {
        Logger.debug(TAG, "sendUserMessage() -> message: $message")

        if (message.isBlank()) {
            return
        }

        val text = message.trim()

        emit(
            OutgoingSocketEvent.USER_MESSAGE,
            json {
                put("text", text)
                put("lang", language)
            }
        )
    }

    override fun sendUserMediaMessage(attachmentType: Attachment.Type, url: String) {
        emit(
            OutgoingSocketEvent.USER_MESSAGE,
            json {
                put(attachmentType.key, url)
            }
        )
    }

    override fun sendUserFeedback(rating: Int, chatId: Long) {
        emit(
            OutgoingSocketEvent.USER_FEEDBACK,
            json {
                put("r", rating)
                put("chat_id", chatId)
            }
        )
    }

    override fun sendUserLocation(id: String, userLocation: UserLocation) {
        Logger.debug(TAG, "sendUserLocation() -> userLocation: $userLocation")

        emit(
            OutgoingSocketEvent.USER_LOCATION,
            json {
                put("id", id)
                put("provider", userLocation.provider)
                put("latitude", userLocation.latitude)
                put("longitude", userLocation.longitude)
                put("bearing", userLocation.bearing)
                put("bearingAccuracyDegrees", userLocation.bearingAccuracyDegrees)
                put("xAccuracyMeters", userLocation.xAccuracyMeters)
                put("yAccuracyMeters", userLocation.yAccuracyMeters)
                put("speed", userLocation.speed)
                put("speedAccuracyMetersPerSecond", userLocation.speedAccuracyMetersPerSecond)
            }
        )
    }

    override fun sendMessage(webRTC: WebRTC?, action: Message.Action?) {
        Logger.debug(TAG, "sendMessage() -> webRTC: $webRTC, action: $action")

        val messageObject = JSONObject()

        try {
            if (webRTC != null) {
                messageObject.put("rtc", json {
                    put("type", webRTC.type.value)

                    if (!webRTC.sdp.isNullOrBlank()) {
                        put("sdp", webRTC.sdp)
                    }

                    if (!webRTC.id.isNullOrBlank()) {
                        put("id", webRTC.id)
                    }

                    webRTC.label?.let { label ->
                        put("label", label)
                    }

                    if (!webRTC.candidate.isNullOrBlank()) {
                        put("candidate", webRTC.candidate)
                    }
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

        emit(OutgoingSocketEvent.MESSAGE, messageObject)
    }

    override fun sendMessage(id: String, userLocation: UserLocation) {
        Logger.debug(TAG, "sendMessage() -> id: $id, userLocation: $userLocation")

        emit(
            OutgoingSocketEvent.MESSAGE,
            json {
                put("action", "location")
                put("id", id)
                put("provider", userLocation.provider)
                put("latitude", userLocation.latitude)
                put("longitude", userLocation.longitude)
                put("bearing", userLocation.bearing)
                put("bearingAccuracyDegrees", userLocation.bearingAccuracyDegrees)
                put("xAccuracyMeters", userLocation.xAccuracyMeters)
                put("yAccuracyMeters", userLocation.yAccuracyMeters)
                put("speed", userLocation.speed)
                put("speedAccuracyMetersPerSecond", userLocation.speedAccuracyMetersPerSecond)
            }
        )
    }

    override fun sendFuzzyTaskConfirmation(name: String, email: String, phone: String) {
        emit(
            OutgoingSocketEvent.CONFIRM_FUZZY_TASK,
            json {
                put("name", name)
                put("email", email)
                put("phone", phone)
                put("res", '1')
            }
        )
    }

    override fun sendExternal(callbackData: String?) {
        emit(
            OutgoingSocketEvent.EXTERNAL,
            json {
                put("callback_data", callbackData)
            }
        )
    }

    override fun sendFormInitialize(formId: Long) {
        emit(
            OutgoingSocketEvent.FORM_INIT,
            json {
                put("form_id", formId)
            }
        )
    }

    override fun sendFormFinalize(form: Form, sender: String?) {
        emit(
            OutgoingSocketEvent.FORM_FINAL,
            json {
                if (!sender.isNullOrBlank()) {
                    put("sender", sender)
                }

                put("form_id", form.id)

                val nodes = JSONArray()
                val fields = JSONObject()

                form.fields.forEach { field ->
                    if (field.isFlex) {
                        nodes.put(json { put(field.type.value, field.value ?: "") })
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
            }
        )
    }

    override fun sendCancel() {
        emit(OutgoingSocketEvent.CANCEL)
    }

    override fun sendCancelPendingCall() {
        emit(OutgoingSocketEvent.CANCEL_PENDING_CALL)
    }

    private fun emit(outgoingSocketEvent: OutgoingSocketEvent, jsonObject: JSONObject? = null): Emitter? {
        return socket?.emit(outgoingSocketEvent.value, jsonObject)
    }
    
    private val onConnectListener = Emitter.Listener {
        Logger.debug(TAG, "event [EVENT_CONNECT]")

        socketStateListener?.onConnect()
    }

    private val onOperatorGreetListener = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [OPERATOR_GREET]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        Logger.debug(TAG, "[OPERATOR_GREET] data: $data")

//        val name = data.optString("name")
        val fullName = data.optString("full_name")

        // Url path
        val photo = data.optString("photo")

        val text = data.optString("text")

        generalListener?.onOperatorGreet(fullName, photo, text)
    }

    private val onFormInitListener = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [FORM_INIT]: $args")

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

        generalListener?.onFormInit(form)
    }

    private val onFormFinalListener = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [FORM_FINAL]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        Logger.debug(TAG, "[FORM_FINAL] data: $data")

        val taskJson = data.optJSONObject("task")
        val trackId = taskJson?.getStringOrNull("track_id")
//        val message = data.getStringOrNull("message")
//        val success = data.optBoolean("success", false)

        generalListener?.onFormFinal(text = trackId ?: "")
    }

    private val onOperatorTypingListener = Emitter.Listener {
        Logger.debug(TAG, "event [OPERATOR_TYPING]")
    }

    private val onFeedbackListener = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [FEEDBACK]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        Logger.debug(TAG, "[FEEDBACK] data: $data")

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

            generalListener?.onFeedback(text, ratingButtons)
        }
    }

    private val onUserQueueListener = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [USER_QUEUE]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        Logger.debug(TAG, "[USER_QUEUE] data: $data")

        val count = data.getInt("count")
//            val channel = userQueue.getInt("channel")

        generalListener?.onPendingUsersQueueCount(count = count)
    }

    private val onMessageListener = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [MESSAGE]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        Logger.debug(TAG, "[MESSAGE] data: $data")

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

        Logger.debug(TAG, "replyMarkupJsonObject: $replyMarkupJsonObject")
        Logger.debug(TAG, "action: $action")

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
            val isHandled = generalListener?.onNoResultsFound(text, time)
            if (isHandled == true) return@Listener
        }

        if (fuzzyTask && !text.isNullOrBlank()) {
            val isHandled = generalListener?.onFuzzyTaskOffered(text, time)
            if (isHandled == true) return@Listener
        }

        if (noOnline && !text.isNullOrBlank()) {
            val isHandled = generalListener?.onNoOnlineOperators(text)
            if (isHandled == true) return@Listener
        }

        if (action == Message.Action.CHAT_TIMEOUT && !text.isNullOrBlank()) {
            val isHandled = generalListener?.onChatTimeout(text, time)
            if (isHandled == true) return@Listener
        }

        if (action == Message.Action.OPERATOR_DISCONNECT && !text.isNullOrBlank()) {
            val isHandled = generalListener?.onOperatorDisconnected(text, time)
            if (isHandled == true) return@Listener
        }

        if (action == Message.Action.REDIRECT && !text.isNullOrBlank()) {
            val isHandled = generalListener?.onUserRedirected(text, time)
            if (isHandled == true) return@Listener
        }

        if (rtcJsonObject != null) {
            when (rtcJsonObject.getStringOrNull("type")) {
                WebRTC.Type.START?.value -> {
                    when (action) {
                        Message.Action.CALL_ACCEPT ->
                            webRTCListener?.onWebRTCCallAccept()
                        Message.Action.CALL_REDIRECT ->
                            webRTCListener?.onWebRTCCallRedirect()
                        Message.Action.CALL_REDIAL -> {
                        }
                        else -> {
                        }
                    }
                }
                WebRTC.Type.PREPARE?.value ->
                    webRTCListener?.onWebRTCPrepare()
                WebRTC.Type.READY?.value ->
                    webRTCListener?.onWebRTCReady()
                WebRTC.Type.OFFER?.value -> {
                    val type = WebRTC.Type.by(rtcJsonObject.getString("type"))
                    val sdp = rtcJsonObject.getString("sdp")

                    if (type != null) {
                        webRTCListener?.onWebRTCOffer(WebRTCSessionDescription(type, sdp))
                    }
                }
                WebRTC.Type.ANSWER?.value -> {
                    val type = WebRTC.Type.by(rtcJsonObject.getString("type"))
                    val sdp = rtcJsonObject.getString("sdp")

                    if (type != null) {
                        webRTCListener?.onWebRTCAnswer(WebRTCSessionDescription(type, sdp))
                    }
                }
                WebRTC.Type.CANDIDATE?.value ->
                    webRTCListener?.onWebRTCIceCandidate(
                        WebRTCIceCandidate(
                            sdpMid = rtcJsonObject.getString("id"),
                            sdpMLineIndex = rtcJsonObject.getInt("label"),
                            sdp = rtcJsonObject.getString("candidate")
                        )
                    )
                WebRTC.Type.HANGUP?.value ->
                    webRTCListener?.onWebRTCHangup()
            }
            return@Listener
        }

        if (!data.isNull("queued")) {
            val queued = data.optInt("queued")
            generalListener?.onPendingUsersQueueCount(text, queued)
        }

        val attachments = mutableListOf<Attachment>()
        if (attachmentsJsonArray != null) {
            for (i in 0 until attachmentsJsonArray.length()) {
                val attachment = attachmentsJsonArray[i] as? JSONObject?
                attachments.add(
                    Attachment(
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
                    Attachment.Type.IMAGE to image
                } else if (!audio.isNullOrBlank()) {
                    Attachment.Type.AUDIO to audio
                } else if (!video.isNullOrBlank()) {
                    Attachment.Type.VIDEO to video
                } else if (!document.isNullOrBlank()) {
                    Attachment.Type.DOCUMENT to document
                } else if (!file.isNullOrBlank()) {
                    Attachment.Type.FILE to file
                } else {
                    null
                }
            } else {
                null
            }

            media = Media(
                title = name,
                extension = findEnumBy { it.value == ext },
                urlPath = pair?.second,
                type = pair?.first
            )
        }

        generalListener?.onMessage(
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
//        Logger.debug(TAG, "event [CATEGORY_LIST]")

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

        generalListener?.onCategories(currentCategories.sortedBy { it.config?.order })
    }

    private val onDisconnectListener = Emitter.Listener {
        Logger.debug(TAG, "event [EVENT_DISCONNECT]")

        lastActiveTime = System.currentTimeMillis()

        socketStateListener?.onDisconnect()
    }

}