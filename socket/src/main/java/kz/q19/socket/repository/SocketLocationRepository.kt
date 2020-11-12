package kz.q19.socket.repository

import kz.q19.domain.model.Location

interface SocketLocationRepository {
    fun sendUserLocation(id: String, location: Location)

    fun sendLocationSubscribe()
    fun sendLocationUnsubscribe()
}