package kz.q19.socket.model

import androidx.annotation.Keep

@Keep
class Sender private constructor(
    private val sender: String,
    val delimiter: String = DEFAULT_DELIMITER
) {

    companion object {
        const val DEFAULT_NAMESPACE: String = "user"
        const val DEFAULT_CHANNEL: String = "em"

        const val DEFAULT_DELIMITER: String = ":"
    }

    val namespace: String
    val channel: String
    val id: String

    init {
        val (namespace, channel, id) = sender.split(delimiter)
        this.namespace = namespace
        this.channel = channel
        this.id = id
    }

    fun get(): String {
        return sender
    }

    class Builder {
        private var namespace: String = DEFAULT_NAMESPACE
        private var channel: String = DEFAULT_CHANNEL
        private var id: String? = null

        private var delimiter: String = DEFAULT_DELIMITER

        fun setNamespace(namespace: String): Builder {
            this.namespace = namespace
            return this
        }

        fun setChannel(channel: String): Builder {
            this.channel = channel
            return this
        }

        fun setId(id: String): Builder {
            this.id = id
            return this
        }

        fun setDelimiter(delimiter: String): Builder {
            this.delimiter = delimiter
            return this
        }

        fun build(): Sender? {
            if (id.isNullOrBlank()) {
                return null
            }
            return Sender("$namespace$delimiter$channel$delimiter$id", delimiter)
        }
    }

}