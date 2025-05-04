package ar.edu.itba.ss

import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class Euler(settings: Settings, val deltaT: BigDecimal) : Algorithm {
    var _currentVelocity: BigDecimal = settings.v0
    var _currentPosition: BigDecimal = settings.r0

    val deltaTSquared = settings.deltaT * settings.deltaT

    override val currentVelocity: BigDecimal
        get() = _currentVelocity
    override val currentPosition: BigDecimal
        get() = _currentPosition

    override fun advanceDeltaT(acceleration: BigDecimal) {
        val nextVelocity = calculateNextVelocity(acceleration)
        _currentVelocity = nextVelocity
        _currentPosition = calculateNextPosition(acceleration, nextVelocity)
    }

    private fun calculateNextVelocity(acceleration: BigDecimal): BigDecimal =
        currentVelocity + deltaT * acceleration

    private fun calculateNextPosition(acceleration: BigDecimal, nextVelocity: BigDecimal): BigDecimal =
        currentPosition + deltaT * nextVelocity + deltaTSquared * acceleration / BigDecimal.TWO
}