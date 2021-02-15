package kz.q19.socket.repository

import kz.q19.domain.model.message.CallAction
import kz.q19.domain.model.message.QRTCAction
import kz.q19.domain.model.webrtc.IceCandidate
import kz.q19.domain.model.webrtc.SessionDescription
import kz.q19.socket.model.CallInitialization

interface SocketCallRepository {
    fun sendCallInitialization(callInitialization: CallInitialization)

    fun sendPendingCallCancellation()

    fun sendCallAction(action: CallAction)
    fun sendQRTCAction(action: QRTCAction)
    fun sendLocalSessionDescription(sessionDescription: SessionDescription)
    fun sendLocalIceCandidate(iceCandidate: IceCandidate)

    fun sendUserCallFeedback(rating: Int, chatId: Long)
}