package kz.q19.socket.listener

import kz.q19.domain.model.Location

interface LocationListener {
    fun onLocationUpdate(location: Location)
}