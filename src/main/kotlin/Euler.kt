package ar.edu.itba.ss

import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class Euler(
    val settings: Settings,
    val acceleration: (settings: Settings, currentPosition: BigDecimal, currentVelocity: BigDecimal) -> BigDecimal,
    deltaT: BigDecimal
) : Algorithm {
    private val dT = deltaT

    override var currentVelocity: BigDecimal = settings.v0
        private set
    override var currentPosition: BigDecimal = settings.r0
        private set
    override var currentAcceleration: BigDecimal = acceleration(settings, currentPosition, currentVelocity)
        private set

    override fun advanceDeltaT(accel: BigDecimal) {
        val r0 = currentPosition
        val v0 = currentVelocity
        val a0 = currentAcceleration

        val v1 = v0 + dT * a0
        val r1 = r0 + dT * v1

        currentVelocity = v1
        currentPosition = r1
        currentAcceleration = acceleration(settings, r1, v1)
    }

    companion object {
        const val PRETTY_NAME = "Euler"
    }
}