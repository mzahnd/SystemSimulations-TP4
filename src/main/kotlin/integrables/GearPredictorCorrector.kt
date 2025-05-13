package ar.edu.itba.ss.integrables

import ar.edu.itba.ss.simulation.Settings
import ch.obermuhlner.math.big.DefaultBigDecimalMath.pow
import ch.obermuhlner.math.big.kotlin.bigdecimal.div
import ch.obermuhlner.math.big.kotlin.bigdecimal.plus
import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class GearPredictorCorrector(
    val settings: Settings,
    val acceleration: (settings: Settings, positions: List<BigDecimal>, velocities: List<BigDecimal>) -> List<BigDecimal>
) : AlgorithmN {
    var _r0: List<BigDecimal>
    var _r1: List<BigDecimal>
    var _r2: List<BigDecimal>
    var _r3: List<BigDecimal>
    var _r4: List<BigDecimal>
    var _r5: List<BigDecimal>

    val dT = settings.deltaT
    val dT2 = dT * dT
    val dT2Over2 = (dT * dT) / FACTORIAL[2]
    val dT3Over3 = (dT * dT * dT) / FACTORIAL[3]
    val dT4Over4 = (dT * dT * dT * dT) / FACTORIAL[4]
    val dT5Over5 = (dT * dT * dT * dT * dT) / FACTORIAL[5]

    val kOverM = settings.k / settings.mass
    val gOverM = settings.gamma / settings.mass

    override var currentVelocities: List<BigDecimal>
        private set
    override var currentPositions: List<BigDecimal>
        private set
    override var currentAccelerations: List<BigDecimal>
        private set

    init {
        _r0 = settings.initialPositions
        _r1 = settings.initialVelocities
        _r2 = _r0.indices.map { i -> -(kOverM * _r0[i] + gOverM * _r1[i]) }
        _r3 = _r1.indices.map { i -> -(kOverM * _r1[i] + gOverM * _r2[i]) }
        _r4 = _r2.indices.map { i -> -(kOverM * _r2[i] + gOverM * _r3[i]) }
        _r5 = _r3.indices.map { i -> -(kOverM * _r3[i] + gOverM * _r4[i]) }

        currentPositions = _r0
        currentVelocities = _r1
        currentAccelerations = _r2
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
        val a2 = acceleration(settings, r0p, r1p)
        val deltaR2 = a2.indices.map { i -> (a2[i] - r2p[i]) * dT2 / FACTORIAL[2] }

        // Step 3
        _r0 = correctRn(r0p, deltaR2, 0)
        _r1 = correctRn(r1p, deltaR2, 1)
        _r2 = correctRn(r2p, deltaR2, 2)
        _r3 = correctRn(r3p, deltaR2, 3)
        _r4 = correctRn(r4p, deltaR2, 4)
        _r5 = correctRn(r5p, deltaR2, 5)

        currentPositions = _r0
        currentVelocities = _r1
        currentAccelerations = _r2
    }

    private fun predictRn(order: Int): List<BigDecimal> {
        return when (order) {
            0 -> _r0.indices.map { i ->
                _r0[i] + _r1[i] * dT + _r2[i] * dT2Over2 +
                        _r3[i] * dT3Over3 + _r4[i] * dT4Over4 + _r5[i] * dT5Over5
            }
            1 -> _r1.indices.map { i ->
                _r1[i] + _r2[i] * dT + _r3[i] * dT2Over2 + _r4[i] * dT3Over3 + _r5[i] * dT4Over4
            }
            2 -> _r2.indices.map { i ->
                _r2[i] + _r3[i] * dT + _r4[i] * dT2Over2 + _r5[i] * dT3Over3
            }
            3 -> _r3.indices.map { i ->
                _r3[i] + _r4[i] * dT + _r5[i] * dT2Over2
            }
            4 -> _r4.indices.map { i ->
                _r4[i] + _r5[i] * dT
            }
            5 -> _r5
            else -> throw IllegalArgumentException("Invalid order $order")
        }
    }

    private fun correctRn(
        rnp: List<BigDecimal>,
        deltaR2: List<BigDecimal>,
        order: Int
    ): List<BigDecimal> {
        return rnp.indices.map { i ->
            rnp[i] + ALPHAS[order] * deltaR2[i] * FACTORIAL[order] / pow(dT, order.toLong())
        }
    }

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