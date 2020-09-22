@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package kz.q19.socket

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kz.q19.domain.*
import kz.q19.domain.webrtc.IceCandidate
import kz.q19.domain.webrtc.SessionDescription
import kz.q19.domain.webrtc.WebRTC
import kz.q19.utils.enum.findEnumBy
import kz.q19.utils.json.getAsMutableList
import kz.q19.utils.json.getLongOrNull
import kz.q19.utils.json.getStringOrNull
import kz.q19.utils.json.json
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class SocketClient private constructor() {

    companion object {
        private const val TAG = "SocketClient"
    }

    private var socket: Socket? = null
    private var language: String? = null
    var listener: Listener? = null

    fun setLanguage(language: String) {
        this.language = language
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
                    type = findEnumBy { it.value == formFieldJson.getString("type") }
                        ?: Form.Field.Type.TEXT,
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

    fun start(url: String, language: String) {
        setLanguage(language)

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

    fun callOperator(callType: CallType, scope: String? = null, language: String? = null) {
        when (callType) {
            CallType.TEXT -> {
                socket?.emit("initialize", json {
                    put("video", false)
                    if (!scope.isNullOrBlank()) {
                        put("scope", scope)
                    }
                    put("lang", fetchLanguage(language))
                })
            }
            CallType.AUDIO -> {
                socket?.emit("initialize", json {
                    put("audio", true)
                    if (!scope.isNullOrBlank()) {
                        put("scope", scope)
                    }
                    put("lang", fetchLanguage(language))
                })
            }
            CallType.VIDEO -> {
                socket?.emit("initialize", json {
                    put("video", true)
                    if (!scope.isNullOrBlank()) {
                        put("scope", scope)
                    }
                    put("lang", fetchLanguage(language))
                })
            }
        }
    }

    fun getBasicCategories(language: String? = null) {
        getCategories(Category.NO_PARENT_ID, language)
    }

    fun getCategories(parentId: Long, language: String? = null) {
//        Logger.debug(TAG, "requestCategories: $parentId")

        socket?.emit("user_dashboard", json {
            put("action", "get_category_list")
            put("parent_id", parentId)
            put("lang", fetchLanguage(language))
        })
    }

    fun getResponse(id: Int, language: String? = null) {
//        Logger.debug(TAG, "requestResponse: $id")

        socket?.emit("user_dashboard", json {
            put("action", "get_response")
            put("id", id)
            put("lang", fetchLanguage(language))
        })
    }

    fun sendFeedback(rating: Int, chatId: Long) {
        Logger.debug(TAG, "sendFeedback: $rating, $chatId")

        socket?.emit("user_feedback", json {
            put("r", rating)
            put("chat_id", chatId)
        })
    }

    fun sendUserMessage(message: String, language: String? = null) {
        Logger.debug(TAG, "sendUserMessage: $message")

        socket?.emit("user_message", json {
            put("text", message)
            put("lang", fetchLanguage(language))
        })
    }

    fun sendUserMediaMessage(mediaType: String, url: String) {
        socket?.emit("user_message", json {
            put(mediaType, url)
        })
    }

    fun sendMessage(
        webRTC: WebRTC? = null,
        action: Message.Action? = null,
        language: String? = null
    ): Emitter? {
        if (webRTC == null || action == null) {
            return null
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

            messageObject.put("lang", fetchLanguage(language))
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        Logger.debug(TAG, "sendMessage: $messageObject")

        return socket?.emit("message", messageObject)
    }

    fun sendFuzzyTaskConfirmation(name: String, email: String, phone: String) {
        socket?.emit("confirm_fuzzy_task", json {
            put("name", name)
            put("email", email)
            put("phone", phone)
            put("res", '1')
        })
    }

    fun sendUserLanguage(language: String) {
        setLanguage(language)

        socket?.emit("user_language", json {
            put("language", language)
        })
    }

    fun sendExternal(callbackData: String?) {
        Logger.debug(TAG, "sendExternal: $callbackData")

        socket?.emit("external", json {
            put("callback_data", callbackData)
        })
    }

    fun sendFormInit(formId: Long) {
        socket?.emit("form_init", json {
            put("form_id", formId)
        })
    }

    fun sendFormFinal(form: Form) {
        Logger.debug(TAG, "sendFormFinal() -> form: $form")

        socket?.emit("form_final", json {
            put("form_id", form.id)

            val nodes = JSONArray()
            val fields = JSONObject()

            form.fields.forEach { field ->
                Logger.debug(TAG, "sendFormFinal() -> forEach: $field")

                if (field.isFlex) {
                    nodes.put(json { put(field.type.value, field.value ?: "") })
                } else {
                    val title = field.title
                    if (!title.isNullOrBlank()) {
                        fields.put(title, json { put(field.type.value, field.value) })
                    }
                }
            }

            Logger.debug(TAG, "sendFormFinal() -> nodes: $nodes")
            Logger.debug(TAG, "sendFormFinal() -> fields: $fields")

            put("form_data", json {
                put("nodes", nodes)
                put("fields", fields)
            })
        })
    }

    fun sendCancel() {
        Logger.debug(TAG, "sendCancel")

        socket?.emit("cancel", json {
        })
    }

    fun cancelPendingCall() {
        Logger.debug(TAG, "cancelPendingCall")

        socket?.emit("cancel_pending_call")
    }

    fun release() {
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

    private fun fetchLanguage(language: String?): String? {
        return if (!language.isNullOrBlank()) {
            language
        } else if (!this.language.isNullOrBlank()) {
            this.language
        } else {
            null
        }
    }

    interface Listener {
        fun onConnect()

//        fun onCall(type: String, media: String, operator: String, instance: String)
        fun onOperatorGreet(fullName: String, photoUrl: String?, text: String)
        fun onFormInit(form: Form)
        fun onFormFinal(text: String)
        fun onFeedback(text: String, ratingButtons: List<RatingButton>)
        fun onPendingUsersQueueCount(text: String? = null, count: Int)
        fun onNoOnlineOperators(text: String): Boolean
        fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean
        fun onNoResultsFound(text: String, timestamp: Long): Boolean
        fun onChatTimeout(text: String, timestamp: Long): Boolean
        fun onOperatorDisconnected(text: String, timestamp: Long): Boolean
        fun onUserRedirected(text: String, timestamp: Long): Boolean

        fun onWebRTCCallAccept()
        fun onWebRTCPrepare()
        fun onWebRTCReady()
        fun onWebRTCAnswer(sessionDescription: SessionDescription)
        fun onWebRTCOffer(sessionDescription: SessionDescription)
        fun onWebRTCIceCandidate(iceCandidate: IceCandidate)
        fun onWebRTCHangup()

        fun onTextMessage(
            text: String?,
            replyMarkup: Message.ReplyMarkup? = null,
            attachments: List<Attachment>? = null,
            form: Form? = null,
            timestamp: Long
        )
        fun onAttachmentMessage(
            attachment: Attachment,
            replyMarkup: Message.ReplyMarkup? = null,
            timestamp: Long
        )

        fun onCategories(categories: List<Category>)

        fun onDisconnect()
    }

}