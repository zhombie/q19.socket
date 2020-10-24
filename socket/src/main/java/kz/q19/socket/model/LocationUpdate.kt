package kz.q19.socket.model

data class LocationUpdate(
    val coordinates: Coordinates? = null
) {

    data class Coordinates(
        val longitude: Double? = null,
        val latitude: Double? = null
    )

}