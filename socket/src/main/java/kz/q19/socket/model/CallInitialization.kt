package kz.q19.socket.model

import kz.q19.domain.model.call.CallType
import kz.q19.domain.model.geo.Location
import kz.q19.domain.model.language.Language

data class CallInitialization constructor(
    val callType: CallType,
    val userId: Long? = null,
    val domain: String? = null,
    val topic: String? = null,
    val location: Location? = null,
    val device: Device? = null,
    val serviceCode: String? = null,
    val language: Language
) {

    data class Device constructor(
        val os: String? = null,
        val osVersion: String? = null,
        val name: String? = null,
        val appVersion: String? = null,
        val mobileOperator: String? = null,
        val battery: Battery? = null
    ) {

        data class Battery constructor(
            val percentage: Double? = null,
            val isCharging: Boolean? = null,
            val temperature: Float? = null
        )

    }

}