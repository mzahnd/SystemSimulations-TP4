package ar.edu.itba.ss

import ch.obermuhlner.math.big.kotlin.bigdecimal.div
import ch.obermuhlner.math.big.kotlin.bigdecimal.minus
import ch.obermuhlner.math.big.kotlin.bigdecimal.plus
import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class Beeman(
    val settings: Settings,
    val acceleration: (settings: Settings, currentPosition: BigDecimal, currentVelocity: BigDecimal) -> BigDecimal
) : Algorithm {
    var _previousPosition: BigDecimal
    var _nextPosition: BigDecimal

    var _previousVelocity: BigDecimal
    var _nextVelocity: BigDecimal

    var _previousAcceleration: BigDecimal

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

    override var currentVelocity: BigDecimal
        private set
    override var currentPosition: BigDecimal
        private set
    override var currentAcceleration: BigDecimal
        private set

    init {
        val ri = settings.r0
        val vi = settings.v0
        val ai = acceleration(settings, ri, vi)

        val euler = Euler(settings, acceleration, -1 * dT)
        euler.advanceDeltaT(ai)

        val riTMinusDt = euler.currentPosition
        val viTMinusDt = euler.currentVelocity
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

        _previousPosition = riTMinusDt
        currentPosition = ri
        _nextPosition = riTPlusDt

        _previousVelocity = viTMinusDt
        currentVelocity = vi
        _nextVelocity = viTPlusDt

        _previousAcceleration = aiMinusDt
        currentAcceleration = ai
    }

    override fun advanceDeltaT(accel: BigDecimal) {
        val ri = currentPosition
        val vi = currentVelocity
        val ai = currentAcceleration
        val aiTMinusDt = _previousAcceleration
        // Predict
        val riTPlusDt = calculateNextPosition(ri, vi, ai, aiTMinusDt)
        val viTPlusDtPredicted = predictNextVelocity(vi, ai, aiTMinusDt)
        // Real acceleration
        val aiTPlusDt = acceleration(settings, riTPlusDt, viTPlusDtPredicted)
        // Correct
        val viTPlusDt = correctVelocity(vi, aiTPlusDt, ai, aiTMinusDt)
        // Shift to current values, and correct v
        _previousPosition = ri
        currentPosition = riTPlusDt
        _previousAcceleration = ai
        currentAcceleration = aiTPlusDt
        _previousVelocity = vi
        currentVelocity = viTPlusDt
        // Predict next step
        _nextPosition = calculateNextPosition(
            currentPosition,
            currentVelocity,
            currentAcceleration,
            _previousAcceleration,
        )
        _nextVelocity = predictNextVelocity(
            currentVelocity,
            currentAcceleration,
            _previousAcceleration,
        )
    }

    // x(t + dT)
    private fun calculateNextPosition(
        x: BigDecimal,
        v: BigDecimal,
        a: BigDecimal,
        aMinusDt: BigDecimal
    ) =
        x + v * dT + twoOverThree * a * dT2 - oneOverSix * aMinusDt * dT2

    private fun predictNextVelocity(
        v: BigDecimal,
        a: BigDecimal,
        aMinusDt: BigDecimal
    ): BigDecimal =
        v + threeOverTwo * a * dT - oneOverTwo * aMinusDt * dT

    private fun correctVelocity(
        v: BigDecimal,
        aPlusDt: BigDecimal,
        a: BigDecimal,
        aMinusDt: BigDecimal
    ): BigDecimal =
        v + oneOverThree * aPlusDt * dT + fiveOverSix * a * dT - oneOverSix * aMinusDt * dT

    companion object {
        const val PRETTY_NAME = "Beeman"
    }
}