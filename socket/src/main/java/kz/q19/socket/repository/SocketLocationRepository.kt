package kz.q19.socket.repository

import kz.q19.socket.model.UserLocation

interface SocketLocationRepository {
    fun sendUserLocation(id: String, userLocation: UserLocation)

    fun sendLocationSubscribe()
}