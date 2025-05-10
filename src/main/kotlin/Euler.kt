package ar.edu.itba.ss

import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class Euler(settings: Settings, deltaT: BigDecimal) : Algorithm {
    var _currentVelocity: BigDecimal = settings.v0
    var _currentPosition: BigDecimal = settings.r0
    var _currentAcceleration: BigDecimal = BigDecimal.ZERO

    private val dT = deltaT
    private val dT2 = dT * dT

    override val currentVelocity: BigDecimal
        get() = _currentVelocity
    override val currentPosition: BigDecimal
        get() = _currentPosition
    override val currentAcceleration: BigDecimal
        get() = _currentAcceleration

    override fun advanceDeltaT(acceleration: BigDecimal) {
        val r0 = _currentPosition
        val v0 = _currentVelocity

        val v1 = v0 + dT * acceleration
        val r1 = r0 + dT * v0

        _currentVelocity = v1
        _currentPosition = r1
        _currentAcceleration = acceleration
    }

    companion object {
        const val PRETTY_NAME = "Euler"
    }
}