package ar.edu.itba.ss.integrables

import java.math.BigDecimal

interface AlgorithmN {
    val currentVelocities: List<BigDecimal>
    val currentPositions: List<BigDecimal>
    val currentAccelerations: List<BigDecimal>

    fun advanceDeltaT()
}