package ar.edu.itba.ss

import ch.obermuhlner.math.big.kotlin.bigdecimal.div
import ch.obermuhlner.math.big.kotlin.bigdecimal.minus
import ch.obermuhlner.math.big.kotlin.bigdecimal.plus
import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class Verlet(settings: Settings, acceleration: (settings: Settings, currentPosition: BigDecimal, currentVelocity: BigDecimal) -> BigDecimal) : Algorithm {
    var _previousPosition: BigDecimal
    var _nextPosition: BigDecimal

    var _currentVelocity: BigDecimal
    var _currentPosition: BigDecimal

    var _currentAcceleration: BigDecimal

    val deltaTSquared = settings.deltaT * settings.deltaT
    val twiceDeltaT = BigDecimal.TWO * settings.deltaT

    override val currentVelocity: BigDecimal
        get() = _currentVelocity
    override val currentPosition: BigDecimal
        get() = _currentPosition
    override val currentAcceleration: BigDecimal
        get() = _currentAcceleration

    init {
        val initialAcceleration = acceleration(settings, settings.r0, settings.v0)
        val euler = Euler(settings, -1 * settings.deltaT)
        euler.advanceDeltaT(initialAcceleration)

        _previousPosition = euler.currentPosition // This is the "-dT" position
        _currentPosition = settings.r0
        _nextPosition = this.calculateNextPosition(initialAcceleration)
        _currentVelocity = settings.v0
        _currentAcceleration = euler.currentAcceleration
    }

    override fun advanceDeltaT(acceleration: BigDecimal) {
        _previousPosition = _currentPosition
        _currentPosition = _nextPosition
        _nextPosition = calculateNextPosition(acceleration)
        _currentVelocity = calculateCurrentVelocity()
        _currentAcceleration = acceleration
    }

    // r(t + dT)
    private fun calculateNextPosition(acceleration: BigDecimal) =
        BigDecimal.TWO * _currentPosition - _previousPosition + deltaTSquared * acceleration

    // v(t)
    private fun calculateCurrentVelocity(): BigDecimal =
        (_nextPosition - _previousPosition) / twiceDeltaT

    companion object {
        const val PRETTY_NAME = "Verlet"
    }
}