package kz.q19.socket.model

data class UserLocation(
    val provider: String? = null,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float? = null,
    val bearingAccuracyDegrees: Float,
    val xAccuracyMeters: Float? = null,
    val yAccuracyMeters: Float? = null,
    val speed: Float? = null,
    val speedAccuracyMetersPerSecond: Float? = null
)