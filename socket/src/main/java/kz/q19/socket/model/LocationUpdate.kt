package kz.q19.socket.model

import androidx.annotation.Keep

@Keep
data class LocationUpdate constructor(
    val gpsCode: Long,
    val x: Double,
    val y: Double
)