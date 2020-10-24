package kz.q19.socket.listener

import kz.q19.socket.model.LocationUpdate

interface LocationListener {
    fun onLocationUpdate(locationUpdate: LocationUpdate)
}