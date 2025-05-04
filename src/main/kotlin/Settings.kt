package ar.edu.itba.ss

import java.io.File
import java.math.BigDecimal
import kotlin.random.Random

data class Settings(
    val outputFile: File,
    val random: Random,
    //
    val deltaT: BigDecimal,
    val mass: BigDecimal,
    val k: Long,
    val gamma: Long,
    val simulationTime: BigDecimal,
    val r0: BigDecimal,
    val v0: BigDecimal,
    val amplitude: Long,
    val seed: Long,
)
