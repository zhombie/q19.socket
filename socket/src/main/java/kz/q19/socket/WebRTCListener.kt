package kz.q19.socket

import kz.q19.domain.model.webrtc.WebRTCIceCandidate
import kz.q19.domain.model.webrtc.WebRTCSessionDescription

abstract class WebRTCListener {
    abstract fun onWebRTCCallAccept()
    abstract fun onWebRTCPrepare()
    abstract fun onWebRTCReady()
    abstract fun onWebRTCAnswer(webRTCSessionDescription: WebRTCSessionDescription)
    abstract fun onWebRTCOffer(webRTCSessionDescription: WebRTCSessionDescription)
    abstract fun onWebRTCIceCandidate(webRTCIceCandidate: WebRTCIceCandidate)
    abstract fun onWebRTCHangup()
}