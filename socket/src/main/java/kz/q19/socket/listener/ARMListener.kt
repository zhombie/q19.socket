package kz.q19.socket.listener

import kz.q19.socket.model.Card102Status
import kz.q19.socket.model.LocationUpdate

interface ARMListener {
    fun onCard102Update(card102Status: Card102Status)
    fun onLocationUpdate(locationUpdates: List<LocationUpdate>)
}