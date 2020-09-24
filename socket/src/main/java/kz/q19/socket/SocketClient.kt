@file:Suppress("unused")

package kz.q19.socket

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kz.q19.common.preferences.PreferencesProvider
import kz.q19.domain.model.*
import kz.q19.domain.model.webrtc.IceCandidate
import kz.q19.domain.model.webrtc.SessionDescription
import kz.q19.domain.model.webrtc.WebRTC
import kz.q19.utils.enum.findEnumBy
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

    private var listener: Listener? = null

    override fun connect(url: String) {
        val options = IO.Options()
        options.reconnection = true
        options.reconnectionAttempts = 3

        socket = IO.socket(url, options)

        socket?.on(Socket.EVENT_CONNECT, onConnect)
        socket?.on("operator_greet", onOperatorGreet)
        socket?.on("form_init", onFormInit)
        socket?.on("form_final", onFormFinal)
        socket?.on("feedback", onFeedback)
        socket?.on("user_queue", onUserQueue)
        socket?.on("operator_typing", onOperatorTyping)
        socket?.on("message", onMessage)
        socket?.on("category_list", onCategoryList)
        socket?.on(Socket.EVENT_DISCONNECT, onDisconnect)

        socket?.connect()
    }

    override fun setListener(listener: Listener?) {
        this.listener = listener
    }

    override fun initializeCall(callType: CallType, language: Language, scope: String?) {
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
        emit(
            OutgoingSocketEvent.USER_LANGUAGE,
            json {
                put("language", language.key)
            }
        )
    }

    override fun sendUserMessage(message: String) {
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

    override fun sendMessage(webRTC: WebRTC?, action: Message.Action?) {
        if (webRTC == null || action == null) {
            return
        }

        val messageObject = JSONObject()

        try {
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

            messageObject.put("action", action.value)

            messageObject.put("lang", language)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        Logger.debug(TAG, "sendMessage: $messageObject")

        emit(OutgoingSocketEvent.MESSAGE, messageObject)
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

    override fun sendFormFinalize(form: Form) {
        emit(
            OutgoingSocketEvent.FORM_FINAL,
            json {
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
    
    override fun release() {
//        socket?.off("call", onCall)
        socket?.off("operator_greet", onOperatorGreet)
        socket?.off("form_init", onFormInit)
        socket?.off("feedback", onFeedback)
        socket?.off("user_queue", onUserQueue)
        socket?.off("operator_typing", onOperatorTyping)
        socket?.off("message", onMessage)
        socket?.off("category_list", onCategoryList)
        socket?.disconnect()
        socket = null
    }

    private val onConnect = Emitter.Listener {
        Logger.debug(TAG, "event [EVENT_CONNECT]")

        listener?.onConnect()
    }

    private val onOperatorGreet = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [OPERATOR_GREET]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        Logger.debug(TAG, "[OPERATOR_GREET] data: $data")

//        val name = data.optString("name")
        val fullName = data.optString("full_name")

        // Url path
        val photo = data.optString("photo")

        val text = data.optString("text")

        listener?.onOperatorGreet(fullName, photo, text)
    }

    private val onFormInit = Emitter.Listener { args ->
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

        listener?.onFormInit(form)
    }

    private val onFormFinal = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [FORM_FINAL]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        Logger.debug(TAG, "[FORM_FINAL] data: $data")

        val taskJson = data.optJSONObject("task")
        val trackId = taskJson?.getStringOrNull("track_id")
//        val message = data.getStringOrNull("message")
//        val success = data.optBoolean("success", false)

        listener?.onFormFinal(text = trackId ?: "")
    }

    private val onOperatorTyping = Emitter.Listener {
        Logger.debug(TAG, "event [OPERATOR_TYPING]")
    }

    private val onFeedback = Emitter.Listener { args ->
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

            listener?.onFeedback(text, ratingButtons)
        }
    }

    private val onUserQueue = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [USER_QUEUE]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        Logger.debug(TAG, "[USER_QUEUE] data: $data")

        val count = data.getInt("count")
//            val channel = userQueue.getInt("channel")

        listener?.onPendingUsersQueueCount(count = count)
    }

    private val onMessage = Emitter.Listener { args ->
//        Logger.debug(TAG, "event [MESSAGE]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        Logger.debug(TAG, "[MESSAGE] data: $data")

        val text = data.getStringOrNull("text")?.trim()
        val noOnline = data.optBoolean("no_online")
        val noResults = data.optBoolean("no_results")
//        val id = message.optString("id")
        val action = findEnumBy<Message.Action> { it.value == data.getStringOrNull("action") }
        val time = data.optLong("time")
        val sender = data.getStringOrNull("sender")
        val from = data.getStringOrNull("from")
        val media = data.optJSONObject("media")
        val rtc = data.optJSONObject("rtc")
        val fuzzyTask = data.optBoolean("fuzzy_task")
//        val form = message.optJSONObject("form")
        val attachmentsJson = data.optJSONArray("attachments")
        val replyMarkupJson = data.optJSONObject("reply_markup")
        val formJson = data.optJSONObject("form")

        Logger.debug(TAG, "replyMarkupJson: $replyMarkupJson")

        var replyMarkup: Message.ReplyMarkup? = null
        if (replyMarkupJson != null) {
            val rows = mutableListOf<List<Message.ReplyMarkup.Button>>()

            val inlineKeyboard = replyMarkupJson.optJSONArray("inline_keyboard")
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
        if (formJson != null && formJson.has("id")) {
            form = Form(
                id = formJson.optLong("id"),
                title = formJson.getStringOrNull("title"),
                prompt = formJson.getStringOrNull("prompt")
            )
        }

        if (noResults && from.isNullOrBlank() && sender.isNullOrBlank() && action == null && !text.isNullOrBlank()) {
            val isHandled = listener?.onNoResultsFound(text, time)
            if (isHandled == true) return@Listener
        }

        if (fuzzyTask && !text.isNullOrBlank()) {
            val isHandled = listener?.onFuzzyTaskOffered(text, time)
            if (isHandled == true) return@Listener
        }

        if (noOnline && !text.isNullOrBlank()) {
            val isHandled = listener?.onNoOnlineOperators(text)
            if (isHandled == true) return@Listener
        }

        if (action == Message.Action.CHAT_TIMEOUT && !text.isNullOrBlank()) {
            val isHandled = listener?.onChatTimeout(text, time)
            if (isHandled == true) return@Listener
        }

        if (action == Message.Action.OPERATOR_DISCONNECT && !text.isNullOrBlank()) {
            val isHandled = listener?.onOperatorDisconnected(text, time)
            if (isHandled == true) return@Listener
        }

        if (action == Message.Action.REDIRECT && !text.isNullOrBlank()) {
            val isHandled = listener?.onUserRedirected(text, time)
            if (isHandled == true) return@Listener
        }

        if (rtc != null) {
            when (rtc.getStringOrNull("type")) {
                WebRTC.Type.START?.value -> {
                    when (action) {
                        Message.Action.CALL_ACCEPT -> listener?.onWebRTCCallAccept()
                        Message.Action.CALL_REDIRECT -> listener?.onWebRTCCallAccept()
                        Message.Action.CALL_REDIAL -> {
                        }
                        else -> {
                        }
                    }
                }
                WebRTC.Type.PREPARE?.value -> listener?.onWebRTCPrepare()
                WebRTC.Type.READY?.value -> listener?.onWebRTCReady()
                WebRTC.Type.OFFER?.value -> {
                    val type = WebRTC.Type.by(rtc.getString("type"))
                    val sdp = rtc.getString("sdp")

                    type?.let {
                        listener?.onWebRTCOffer(SessionDescription(type, sdp))
                    }
                }
                WebRTC.Type.ANSWER?.value -> {
                    val type = WebRTC.Type.by(rtc.getString("type"))
                    val sdp = rtc.getString("sdp")

                    type?.let {
                        listener?.onWebRTCAnswer(SessionDescription(type, sdp))
                    }
                }
                WebRTC.Type.CANDIDATE?.value ->
                    listener?.onWebRTCIceCandidate(
                        IceCandidate(
                            sdpMid = rtc.getString("id"),
                            sdpMLineIndex = rtc.getInt("label"),
                            sdp = rtc.getString("candidate")
                        )
                    )
                WebRTC.Type.HANGUP?.value -> listener?.onWebRTCHangup()
            }
            return@Listener
        }

        if (!data.isNull("queued")) {
            val queued = data.optInt("queued")
            listener?.onPendingUsersQueueCount(text, queued)
            listener?.onTextMessage(
                text = text,
                replyMarkup = replyMarkup,
                form = form,
                timestamp = time
            )
        } else {
            if (attachmentsJson != null) {
                val attachments = mutableListOf<Attachment>()
                for (i in 0 until attachmentsJson.length()) {
                    val attachment = attachmentsJson[i] as? JSONObject?
                    attachments.add(
                        Attachment(
                            title = attachment?.getStringOrNull("title"),
                            extension = findEnumBy { it.value == attachment?.getStringOrNull("ext") },
                            type = findEnumBy { it.key == attachment?.getStringOrNull("type") },
                            urlPath = attachment?.getStringOrNull("url")
                        )
                    )
                }

                listener?.onTextMessage(
                    text = text,
                    replyMarkup = replyMarkup,
                    attachments = attachments,
                    form = form,
                    timestamp = time
                )
            } else {
                listener?.onTextMessage(
                    text = text,
                    replyMarkup = replyMarkup,
                    form = form,
                    timestamp = time
                )
            }
        }

        if (media != null) {
            val image = media.getStringOrNull("image")
            val audio = media.getStringOrNull("audio")
            val file = media.getStringOrNull("file")
            val name = media.getStringOrNull("name")
            val ext = media.getStringOrNull("ext")

            if (!image.isNullOrBlank() && !ext.isNullOrBlank()) {
                listener?.onAttachmentMessage(
                    attachment = Attachment(
                        urlPath = image,
                        title = name,
                        extension = findEnumBy { it.value == ext }
                    ),
                    replyMarkup = replyMarkup,
                    timestamp = time
                )
            }

            if (!audio.isNullOrBlank() && !ext.isNullOrBlank()) {
                listener?.onAttachmentMessage(
                    attachment = Attachment(
                        urlPath = audio,
                        title = name,
                        extension = findEnumBy { it.value == ext }
                    ),
                    replyMarkup = replyMarkup,
                    timestamp = time
                )
            }

            if (!file.isNullOrBlank() && !ext.isNullOrBlank()) {
                listener?.onAttachmentMessage(
                    attachment = Attachment(
                        urlPath = file,
                        title = name,
                        extension = findEnumBy { it.value == ext }
                    ),
                    replyMarkup = replyMarkup,
                    timestamp = time
                )
            }
        }
    }

    private val onCategoryList = Emitter.Listener { args ->
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

        listener?.onCategories(currentCategories.sortedBy { it.config?.order })
    }

    private val onDisconnect = Emitter.Listener {
//        Logger.debug(TAG, "event [EVENT_DISCONNECT]")

        listener?.onDisconnect()
    }

}