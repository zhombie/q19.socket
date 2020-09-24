package kz.q19.socket

import kz.q19.domain.model.webrtc.IceCandidate
import kz.q19.domain.model.webrtc.SessionDescription

interface WebRTCListener {
    fun onWebRTCCallAccept()
    fun onWebRTCPrepare()
    fun onWebRTCReady()
    fun onWebRTCAnswer(sessionDescription: SessionDescription)
    fun onWebRTCOffer(sessionDescription: SessionDescription)
    fun onWebRTCIceCandidate(iceCandidate: IceCandidate)
    fun onWebRTCHangup()
}