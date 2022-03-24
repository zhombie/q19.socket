package kz.q19.socket.model

data class LocationUpdate constructor(
    val gpsCode: Long,
    val x: Double,
    val y: Double
)