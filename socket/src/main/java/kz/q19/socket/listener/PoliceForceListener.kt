package kz.q19.socket.listener

import kz.q19.domain.model.Location
import kz.q19.socket.model.Card102Status
import kz.q19.socket.model.LocationUpdate

interface PoliceForceListener {
    fun onCard102Update(card102Status: Card102Status)
    fun onLocationUpdate(location: Location)
    fun onLocationUpdate(locationUpdates: List<LocationUpdate>)
}