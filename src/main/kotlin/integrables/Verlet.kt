package ar.edu.itba.ss.integrables

import ar.edu.itba.ss.simulation.Settings
import ch.obermuhlner.math.big.kotlin.bigdecimal.div
import ch.obermuhlner.math.big.kotlin.bigdecimal.minus
import ch.obermuhlner.math.big.kotlin.bigdecimal.plus
import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class Verlet(
    val settings: Settings,
    val acceleration: (settings: Settings, positions: List<BigDecimal>, velocities: List<BigDecimal>) -> List<BigDecimal>
) : AlgorithmN {

    var previousPositions: List<BigDecimal>

    val dT = settings.deltaT
    val dT2 = dT * dT
    val twiceDeltaT = BigDecimal.TWO * dT

    override var currentVelocities: List<BigDecimal>
        private set
    override var currentPositions: List<BigDecimal>
        private set
    override var currentAccelerations: List<BigDecimal>
        private set

    init {
        val ri = settings.initialPositions
        val vi = settings.initialVelocities
        val a0 = acceleration(settings, ri, vi)

        val euler = Euler(settings, acceleration, -1 * dT)
        euler.advanceDeltaT()

        val riTMinusDt = euler.currentPositions

        previousPositions = riTMinusDt
        currentPositions = ri
        currentVelocities = vi
        currentAccelerations = a0
    }

    override fun advanceDeltaT() {
        val riTMinusDt = previousPositions
        val ri = currentPositions

        val vi = currentVelocities
        val a0 = acceleration(settings, ri, vi)

        val riTPlusDt = calculateNextPosition(ri, riTMinusDt, a0)
        val viTPlusDt = calculateCurrentVelocity(riTMinusDt, riTPlusDt)

        previousPositions = ri
        currentPositions = riTPlusDt
        currentVelocities = viTPlusDt
        currentAccelerations = a0
    }

    // r(t + dT)
    private fun calculateNextPosition(
        currentPositions: List<BigDecimal>,
        previousPositions: List<BigDecimal>,
        accelerations: List<BigDecimal>
    ): List<BigDecimal> {
        require(currentPositions.size == previousPositions.size && currentPositions.size == accelerations.size) {
            "Position lists must have the same size"
        }
        return currentPositions.indices.map { i ->
            BigDecimal.TWO * currentPositions[i] - previousPositions[i] + dT2 * accelerations[i]
        }
    }

    // v(t)
    private fun calculateCurrentVelocity(
        previousPositions: List<BigDecimal>,
        nextPositions: List<BigDecimal>
    ): List<BigDecimal> {
        require(previousPositions.size == nextPositions.size) {
            "Position lists must have the same size"
        }

        return previousPositions.indices.map { i ->
            (nextPositions[i] - previousPositions[i]) / twiceDeltaT
        }
    }

    companion object {
        const val PRETTY_NAME = "Verlet"
    }
}