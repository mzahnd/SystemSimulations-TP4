package ar.edu.itba.ss

import ch.obermuhlner.math.big.DefaultBigDecimalMath.pow
import ch.obermuhlner.math.big.kotlin.bigdecimal.div
import ch.obermuhlner.math.big.kotlin.bigdecimal.plus
import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class GearPredictorCorrector(
    val settings: Settings,
    val acceleration: (settings: Settings, currentPosition: BigDecimal, currentVelocity: BigDecimal) -> BigDecimal
) : Algorithm {
    var _r0: BigDecimal
    var _r1: BigDecimal
    var _r2: BigDecimal
    var _r3: BigDecimal
    var _r4: BigDecimal
    var _r5: BigDecimal

    val dT = settings.deltaT
    val dT2 = dT * dT
    val dT2Over2 = (dT * dT) / FACTORIAL[2]
    val dT3Over3 = (dT * dT * dT) / FACTORIAL[3]
    val dT4Over4 = (dT * dT * dT * dT) / FACTORIAL[4]
    val dT5Over5 = (dT * dT * dT * dT * dT) / FACTORIAL[5]

    override var currentVelocity: BigDecimal
        private set
    override var currentPosition: BigDecimal
        private set
    override var currentAcceleration: BigDecimal
        private set

    init {
        val r = settings.r0
        val v = settings.v0
        // Step 0: Init
        val kOverM = settings.k / settings.mass
        val gOverM = settings.gamma / settings.mass

        _r0 = settings.r0
        _r1 = settings.v0
        _r2 = -(kOverM * _r0 + gOverM * _r1)
        _r3 = -(kOverM * _r1 + gOverM * _r2)
        _r4 = -(kOverM * _r2 + gOverM * _r3)
        _r5 = -(kOverM * _r3 + gOverM * _r4)

        // Step 1
        val r0p = predictRn(0)
        val r1p = predictRn(1)
        val r2p = predictRn(2)
        val r3p = predictRn(3)
        val r4p = predictRn(4)
        val r5p = predictRn(5)

        // Step 2
        val deltaAcceleration = acceleration(settings, r0p, r1p) - r2p
        val deltaR2 = deltaAcceleration * dT2 / 2

        // Step 3
        _r0 = correctRn(r0p, deltaR2, 0)
        _r1 = correctRn(r1p, deltaR2, 1)
        _r2 = correctRn(r2p, deltaR2, 2)
        _r3 = correctRn(r3p, deltaR2, 3)
        _r4 = correctRn(r4p, deltaR2, 4)
        _r5 = correctRn(r5p, deltaR2, 5)

        currentPosition = _r0
        currentVelocity = _r1
        currentAcceleration = _r2
    }

    override fun advanceDeltaT() {
        // Step 1
        val r0p = predictRn(0)
        val r1p = predictRn(1)
        val r2p = predictRn(2)
        val r3p = predictRn(3)
        val r4p = predictRn(4)
        val r5p = predictRn(5)

        // Step 2
        val deltaAcceleration = acceleration(settings, r0p, r1p) - r2p
        val deltaR2 = deltaAcceleration * dT2 / FACTORIAL[2]

        // Step 3
        _r0 = correctRn(r0p, deltaR2, 0)
        _r1 = correctRn(r1p, deltaR2, 1)
        _r2 = correctRn(r2p, deltaR2, 2)
        _r3 = correctRn(r3p, deltaR2, 3)
        _r4 = correctRn(r4p, deltaR2, 4)
        _r5 = correctRn(r5p, deltaR2, 5)

        currentPosition = _r0
        currentVelocity = _r1
        currentAcceleration = _r2
    }

    private fun predictRn(order: Int) =
        when (order) {
            0 -> _r0 + _r1 * dT + _r2 * dT2Over2 + _r3 * dT3Over3 + _r4 * dT4Over4 + _r5 * dT5Over5
            1 -> _r1 + _r2 * dT + _r3 * dT2Over2 + _r4 * dT3Over3 + _r5 * dT4Over4
            2 -> _r2 + _r3 * dT + _r4 * dT2Over2 + _r5 * dT3Over3
            3 -> _r3 + _r4 * dT + _r5 * dT2Over2
            4 -> _r4 + _r5 * dT
            5 -> _r5
            else -> throw IllegalArgumentException("Invalid order $order")
        }

    private fun correctRn(rnp: BigDecimal, deltaR2: BigDecimal, order: Int) =
        rnp + ALPHAS[order] * deltaR2 * FACTORIAL[order] / pow(dT, order.toLong())

    companion object {
        const val PRETTY_NAME = "Gear Predictor-Corrector"

        val ALPHAS = listOf(
            BigDecimal.valueOf(3) / BigDecimal.valueOf(16), // 0
            BigDecimal.valueOf(251) / BigDecimal.valueOf(360), // 1
            BigDecimal.ONE, // 2
            BigDecimal.valueOf(11) / BigDecimal.valueOf(18), // 3
            BigDecimal.ONE / BigDecimal.valueOf(6), // 4
            BigDecimal.ONE / BigDecimal.valueOf(60) // 5
        )

        val FACTORIAL = listOf(
            1, // 0
            1, // 1
            2, // 2
            6, // 3
            24, // 4
            120, // 5
        ).map { BigDecimal.valueOf(it.toLong()) }
    }
}