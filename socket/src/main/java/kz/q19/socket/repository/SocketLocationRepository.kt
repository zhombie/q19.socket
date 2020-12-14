package kz.q19.socket.repository

import kz.q19.domain.model.Location

interface SocketLocationRepository {
    fun sendUserLocation(id: String, location: Location)

    fun sendLocationUpdateSubscribe()
    fun sendLocationUpdateUnsubscribe()

    fun subscribeToCard102Update()
    fun unsubscribeFromCard102Update()
}