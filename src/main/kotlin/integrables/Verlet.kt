package ar.edu.itba.ss.Integrables

import ar.edu.itba.ss.simulation.Settings
import ch.obermuhlner.math.big.kotlin.bigdecimal.div
import ch.obermuhlner.math.big.kotlin.bigdecimal.minus
import ch.obermuhlner.math.big.kotlin.bigdecimal.plus
import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class Verlet(
    val settings: Settings,
    val acceleration: (settings: Settings, currentPosition: BigDecimal, currentVelocity: BigDecimal) -> BigDecimal
) : Algorithm {
    var previousPosition: BigDecimal

    val dT = settings.deltaT
    val dT2 = dT * dT
    val twiceDeltaT = BigDecimal.TWO * dT

    override var currentVelocity: BigDecimal
        private set
    override var currentPosition: BigDecimal
        private set
    override var currentAcceleration: BigDecimal
        private set

    init {
        val ri = settings.r0
        val vi = settings.v0
        val a0 = acceleration(settings, ri, vi)

        val euler = Euler(settings, acceleration, -1 * dT)
        euler.advanceDeltaT()

        val riTMinusDt = euler.currentPosition

        previousPosition = riTMinusDt
        currentPosition = ri
        currentVelocity = vi
        currentAcceleration = a0
    }

    override fun advanceDeltaT() {
        val riTMinusDt = previousPosition
        val ri = currentPosition

        val vi = currentVelocity
        val a0 = acceleration(settings, ri, vi)

        val riTPlusDt = calculateNextPosition(ri, riTMinusDt, a0)
        val viTPlusDt = calculateCurrentVelocity(riTMinusDt, riTPlusDt)

        previousPosition = ri
        currentPosition = riTPlusDt
        currentVelocity = viTPlusDt
        currentAcceleration = a0
    }

    // r(t + dT)
    private fun calculateNextPosition(ri: BigDecimal, riTMinusDt: BigDecimal, acceleration: BigDecimal) =
        BigDecimal.TWO * ri - riTMinusDt + dT2 * acceleration

    // v(t)
    private fun calculateCurrentVelocity(riTMinusDt: BigDecimal, riTPlusDt: BigDecimal): BigDecimal =
        (riTPlusDt - riTMinusDt) / twiceDeltaT

    companion object {
        const val PRETTY_NAME = "Verlet"
    }
}