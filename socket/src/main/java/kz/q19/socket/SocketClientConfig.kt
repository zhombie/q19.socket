package kz.q19.socket

import kz.q19.domain.model.language.Language

object SocketClientConfig {

    private var isLoggingEnabled: Boolean = true
    private var language: Language = Language.DEFAULT

    fun init(isLoggingEnabled: Boolean, language: Language) {
        this.isLoggingEnabled = isLoggingEnabled
        this.language = language
    }

    fun isLoggingEnabled(): Boolean = isLoggingEnabled

    fun getLanguage(): Language = language

}