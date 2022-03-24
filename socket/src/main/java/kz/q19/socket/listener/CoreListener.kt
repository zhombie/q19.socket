package kz.q19.socket.listener

import kz.garage.chat.model.Message

interface CoreListener {
    fun onMessage(message: Message)
}