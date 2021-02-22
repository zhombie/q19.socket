package kz.q19.socket.listener

import kz.q19.socket.model.TaskMessage

interface TaskListener {
    fun onTaskMessage(taskMessage: TaskMessage)
}