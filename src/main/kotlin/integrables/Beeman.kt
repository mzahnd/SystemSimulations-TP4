package ar.edu.itba.ss.integrables

import ar.edu.itba.ss.simulation.Settings
import ch.obermuhlner.math.big.kotlin.bigdecimal.div
import ch.obermuhlner.math.big.kotlin.bigdecimal.minus
import ch.obermuhlner.math.big.kotlin.bigdecimal.plus
import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class Beeman(
    val settings: Settings,
    val acceleration: (settings: Settings, positions: List<BigDecimal>, velocities: List<BigDecimal>) -> List<BigDecimal>
) : AlgorithmN {
    var previousPositions: List<BigDecimal>
    var nextPositions: List<BigDecimal>

    var previousVelocities: List<BigDecimal>
    var nextVelocities: List<BigDecimal>

    var previousAccelerations: List<BigDecimal>

    val dT = settings.deltaT
    val dT2 = dT * dT

    // x(t + dT)
    val twoOverThree = BigDecimal.TWO / BigDecimal.valueOf(3)
    val oneOverSix = BigDecimal.ONE / BigDecimal.valueOf(6)

    // v(t + dT) predicted
    val threeOverTwo = BigDecimal.valueOf(3) / BigDecimal.TWO
    val oneOverTwo = BigDecimal.ONE / BigDecimal.TWO

    // v(t + dT) corrected
    val oneOverThree = BigDecimal.ONE / BigDecimal.valueOf(3)
    val fiveOverSix = BigDecimal.valueOf(5) / BigDecimal.valueOf(6)

    override var currentVelocities: List<BigDecimal>
        private set
    override var currentPositions: List<BigDecimal>
        private set
    override var currentAccelerations: List<BigDecimal>
        private set

    init {
        val ri = settings.initialPositions
        val vi = settings.initialVelocities
        val ai = acceleration(settings, ri, vi)

        val euler = Euler(settings, acceleration, -1 * dT)
        euler.advanceDeltaT()

        val riTMinusDt = euler.currentPositions
        val viTMinusDt = euler.currentVelocities
        val aiMinusDt = acceleration(settings, riTMinusDt, viTMinusDt)

        val riTPlusDt = calculateNextPosition(
            x = ri,
            v = vi,
            a = ai,
            aMinusDt = aiMinusDt,
        )
        val viTPlusDt = predictNextVelocity(
            v = vi,
            a = ai,
            aMinusDt = aiMinusDt,
        )

        previousPositions = riTMinusDt
        currentPositions = ri
        nextPositions = riTPlusDt

        previousVelocities = viTMinusDt
        currentVelocities = vi
        nextVelocities = viTPlusDt

        previousAccelerations = aiMinusDt
        currentAccelerations = ai
    }

    override fun advanceDeltaT() {
        val ri = currentPositions
        val vi = currentVelocities
        val ai = currentAccelerations
        val aiTMinusDt = previousAccelerations
        // Predict
        val riTPlusDt = calculateNextPosition(ri, vi, ai, aiTMinusDt)
        val viTPlusDtPredicted = predictNextVelocity(vi, ai, aiTMinusDt)
        // Real acceleration
        val aiTPlusDt = acceleration(settings, riTPlusDt, viTPlusDtPredicted)
        // Correct
        val viTPlusDt = correctVelocity(vi, aiTPlusDt, ai, aiTMinusDt)
        // Shift to current values, and correct v
        previousPositions = ri
        currentPositions = riTPlusDt
        previousAccelerations = ai
        currentAccelerations = aiTPlusDt
        previousVelocities = vi
        currentVelocities = viTPlusDt
        // Predict next step
        nextPositions = calculateNextPosition(
            currentPositions,
            currentVelocities,
            currentAccelerations,
            previousAccelerations,
        )
        nextVelocities = predictNextVelocity(
            currentVelocities,
            currentAccelerations,
            previousAccelerations,
        )
    }

    // x(t + dT) for all particles
    private fun calculateNextPosition(
        x: List<BigDecimal>,
        v: List<BigDecimal>,
        a: List<BigDecimal>,
        aMinusDt: List<BigDecimal>
    ): List<BigDecimal> {
        require(x.size == v.size && x.size == a.size && x.size == aMinusDt.size) {
            "All input lists must have the same size"
        }

        return x.indices.map { i ->
            x[i] + v[i] * dT + twoOverThree * a[i] * dT2 - oneOverSix * aMinusDt[i] * dT2
        }
    }

    // v(t + dT) predicted for all particles
    private fun predictNextVelocity(
        v: List<BigDecimal>,
        a: List<BigDecimal>,
        aMinusDt: List<BigDecimal>
    ): List<BigDecimal> {
        require(v.size == a.size && v.size == aMinusDt.size) {
            "All input lists must have the same size"
        }

        return v.indices.map { i ->
            v[i] + threeOverTwo * a[i] * dT - oneOverTwo * aMinusDt[i] * dT
        }
    }

    // v(t + dT) corrected for all particles
    private fun correctVelocity(
        v: List<BigDecimal>,
        aPlusDt: List<BigDecimal>,
        a: List<BigDecimal>,
        aMinusDt: List<BigDecimal>
    ): List<BigDecimal> {
        require(v.size == aPlusDt.size && v.size == a.size && v.size == aMinusDt.size) {
            "All input lists must have the same size"
        }

        return v.indices.map { i ->
            v[i] + oneOverThree * aPlusDt[i] * dT +
                    fiveOverSix * a[i] * dT -
                    oneOverSix * aMinusDt[i] * dT
        }
    }

    companion object {
        const val PRETTY_NAME = "Beeman"
    }
}