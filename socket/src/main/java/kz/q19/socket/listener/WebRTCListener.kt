package kz.q19.socket.listener

import kz.q19.domain.model.webrtc.WebRTCIceCandidate
import kz.q19.domain.model.webrtc.WebRTCSessionDescription

interface WebRTCListener {
    fun onWebRTCCallAccept()
    fun onWebRTCCallRedirect()
    fun onWebRTCPrepare()
    fun onWebRTCReady()
    fun onWebRTCAnswer(webRTCSessionDescription: WebRTCSessionDescription)
    fun onWebRTCOffer(webRTCSessionDescription: WebRTCSessionDescription)
    fun onWebRTCIceCandidate(webRTCIceCandidate: WebRTCIceCandidate)
    fun onWebRTCHangup()
}