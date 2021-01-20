package kz.q19.socket.listener

import kz.q19.domain.model.webrtc.IceCandidate
import kz.q19.domain.model.webrtc.SessionDescription

interface WebRTCListener {
    fun onCallAccept()
    fun onCallRedirect()
    fun onCallRedial()
    fun onCallPrepare()
    fun onCallReady()
    fun onCallAnswer(sessionDescription: SessionDescription)
    fun onCallOffer(sessionDescription: SessionDescription)
    fun onRemoteIceCandidate(iceCandidate: IceCandidate)
    fun onPeerHangupCall()
}