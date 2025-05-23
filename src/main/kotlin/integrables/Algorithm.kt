package ar.edu.itba.ss.integrables

import java.math.BigDecimal

interface Algorithm {
    val currentVelocity: BigDecimal
    val currentPosition: BigDecimal
    val currentAcceleration: BigDecimal

    fun advanceDeltaT()
}