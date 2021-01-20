package kz.q19.socket.repository

import kz.q19.domain.model.call.CallType
import kz.q19.domain.model.geo.Location
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.message.CallAction
import kz.q19.domain.model.message.QRTCAction
import kz.q19.domain.model.webrtc.IceCandidate
import kz.q19.domain.model.webrtc.SessionDescription

interface SocketCallRepository {
    fun sendCallInitialization(
        callType: CallType,
        userId: Long? = null,
        domain: String? = null,
        topic: String? = null,
        location: Location? = null,
        language: Language
    )

    fun sendPendingCallCancellation()

    fun sendCallAction(action: CallAction)
    fun sendQRTCAction(action: QRTCAction)
    fun sendLocalSessionDescription(sessionDescription: SessionDescription)
    fun sendLocalIceCandidate(iceCandidate: IceCandidate)

    fun sendUserDialogFeedback(rating: Int, chatId: Long)
}