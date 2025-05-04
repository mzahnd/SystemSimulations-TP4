package ar.edu.itba.ss

import java.math.BigDecimal

interface Algorithm {
    val currentVelocity: BigDecimal
    val currentPosition: BigDecimal

    fun advanceDeltaT(acceleration: BigDecimal)
}