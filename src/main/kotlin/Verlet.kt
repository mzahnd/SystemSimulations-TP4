package ar.edu.itba.ss

import ch.obermuhlner.math.big.kotlin.bigdecimal.div
import ch.obermuhlner.math.big.kotlin.bigdecimal.minus
import ch.obermuhlner.math.big.kotlin.bigdecimal.plus
import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class Verlet(settings: Settings, initialAcceleration: BigDecimal) : Algorithm {
    var _previousPosition: BigDecimal
    var _nextPosition: BigDecimal

    var _currentVelocity: BigDecimal = settings.v0
    var _currentPosition: BigDecimal = settings.r0

    val deltaTSquared = settings.deltaT * settings.deltaT
    val twiceDeltaT = BigDecimal.TWO * settings.deltaT

    override val currentVelocity: BigDecimal
        get() = _currentVelocity
    override val currentPosition: BigDecimal
        get() = _currentPosition

    init {
        val euler = Euler(settings, -1 * settings.deltaT)
        euler.advanceDeltaT(initialAcceleration)
        _previousPosition = euler.currentPosition // This is the "-dT" position
        _nextPosition = this.calculateNextPosition(initialAcceleration)
    }

    override fun advanceDeltaT(acceleration: BigDecimal) {
        val newPreviousPosition = _currentPosition
        val newCurrentPosition = _nextPosition
        val newNextPosition = calculateNextPosition(acceleration)

        _previousPosition = newPreviousPosition
        _currentPosition = newCurrentPosition
        _nextPosition = newNextPosition
        _currentVelocity = calculateCurrentVelocity()
    }

    // r(t + dT)
    private fun calculateNextPosition(acceleration: BigDecimal) =
        BigDecimal.TWO * _currentPosition - _previousPosition + deltaTSquared * acceleration

    // v(t)
    private fun calculateCurrentVelocity(): BigDecimal =
        (_nextPosition - _previousPosition) / twiceDeltaT
}