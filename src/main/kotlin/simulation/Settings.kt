package ar.edu.itba.ss.simulation

import java.io.File
import java.math.BigDecimal
import kotlin.random.Random

data class Settings(
    val outputFile: File,
    val random: Random,
    //
    val deltaT: BigDecimal,
    val mass: BigDecimal,
    val k: Double,
    val gamma: Double,
    val simulationTime: BigDecimal,
    val initialPositions: List<BigDecimal>,
    val initialVelocities: List<BigDecimal>,
    val amplitude: Double,
    val seed: Long,
)
