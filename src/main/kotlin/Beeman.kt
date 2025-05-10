package ar.edu.itba.ss

import ch.obermuhlner.math.big.kotlin.bigdecimal.div
import ch.obermuhlner.math.big.kotlin.bigdecimal.minus
import ch.obermuhlner.math.big.kotlin.bigdecimal.plus
import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class Beeman(
    val settings: Settings,
    acceleration: (settings: Settings, currentPosition: BigDecimal, currentVelocity: BigDecimal) -> BigDecimal
) : Algorithm {
    var _previousPosition: BigDecimal
    var _currentPosition: BigDecimal
    var _nextPosition: BigDecimal

    var _previousVelocity: BigDecimal
    var _currentVelocity: BigDecimal
    var _nextVelocity: BigDecimal

    var _previousAcceleration: BigDecimal
    var _currentAcceleration: BigDecimal

    val deltaTSquared = settings.deltaT * settings.deltaT

    // x(t + dT)
    val twoOverThree = BigDecimal.TWO / BigDecimal.valueOf(3)
    val oneOverSix = BigDecimal.ONE / BigDecimal.valueOf(6)

    // v(t + dT) predicted
    val threeOverTwo = BigDecimal.valueOf(3) / BigDecimal.TWO
    val oneOverTwo = BigDecimal.ONE / BigDecimal.TWO

    // v(t + dT) corrected
    val oneOverThree = BigDecimal.ONE / BigDecimal.valueOf(3)
    val fiveOverSix = BigDecimal.valueOf(5) / BigDecimal.valueOf(6)

    override val currentVelocity: BigDecimal
        get() = _currentVelocity
    override val currentPosition: BigDecimal
        get() = _currentPosition
    override val currentAcceleration: BigDecimal
        get() = _currentAcceleration

    init {
        val euler = Euler(settings, acceleration, -1 * settings.deltaT)
        _currentAcceleration = acceleration(settings, settings.r0, settings.v0)
        euler.advanceDeltaT(_currentAcceleration)

        _previousPosition = euler.currentPosition // "-dT" position
        _currentPosition = settings.r0
        _previousVelocity = euler.currentVelocity // "-dT" velocity
        _currentVelocity = settings.v0

        _previousAcceleration = acceleration(settings, _previousPosition, _previousVelocity)
        euler.advanceDeltaT(_previousAcceleration) // (2 * -dT)

        _nextPosition = this.calculateNextPosition(
            x = _currentPosition,
            v = _currentVelocity,
            a = _currentAcceleration,
            aMinusDt = _previousAcceleration,
            dT = settings.deltaT
        )
        _nextVelocity = this.predictNextVelocity(
            v = _currentVelocity,
            a = _currentAcceleration,
            aMinusDt = _previousAcceleration,
            dT = settings.deltaT
        )
    }

    override fun advanceDeltaT(acceleration: BigDecimal) {
        _previousVelocity = _currentVelocity
        // At this point, we are in a(t + dT) for the predicted nextVelocity
        _nextVelocity = correctVelocity(
            v = _currentVelocity, // v(t)
            aPlusDt = acceleration,
            a = _currentAcceleration,
            aMinusDt = _previousAcceleration,
            dT = settings.deltaT
        )
        _currentVelocity = _nextVelocity

        // Update acceleration to predict v(t + dT)
        _previousAcceleration = _currentAcceleration
        _currentAcceleration = acceleration

        _nextVelocity = predictNextVelocity(
            v = _currentVelocity,
            a = _currentAcceleration,
            aMinusDt = _previousAcceleration,
            dT = settings.deltaT
        )

        _previousPosition = _currentPosition
        _currentPosition = _nextPosition
        _nextPosition = calculateNextPosition(
            x = _currentPosition,
            v = _currentVelocity,
            a = _currentAcceleration,
            aMinusDt = _previousAcceleration,
            dT = settings.deltaT
        )
    }

    // x(t + dT)
    private fun calculateNextPosition(
        x: BigDecimal,
        v: BigDecimal,
        a: BigDecimal,
        aMinusDt: BigDecimal,
        dT: BigDecimal
    ) =
        x + v * dT + twoOverThree * a * deltaTSquared - oneOverSix * aMinusDt * deltaTSquared

    private fun predictNextVelocity(
        v: BigDecimal,
        a: BigDecimal,
        aMinusDt: BigDecimal,
        dT: BigDecimal
    ): BigDecimal =
        v + threeOverTwo * a * dT - oneOverTwo * aMinusDt * dT

    private fun correctVelocity(
        v: BigDecimal,
        aPlusDt: BigDecimal,
        a: BigDecimal,
        aMinusDt: BigDecimal,
        dT: BigDecimal
    ): BigDecimal =
        v + oneOverThree * aPlusDt * dT + fiveOverSix * a * dT - oneOverSix * aMinusDt * dT

    companion object {
        const val PRETTY_NAME = "Beeman"
    }
}