package ar.edu.itba.ss.simulation

import java.io.File
import java.math.BigDecimal
import kotlin.random.Random

interface SimulationSettings {
    val outputFile: File
    val random: Random
    val deltaT: BigDecimal
    val mass: BigDecimal
    val k: Double
    val gamma: Double
    val simulationTime: BigDecimal
    val initialPositions: List<BigDecimal>
    val initialVelocities: List<BigDecimal>
    val amplitude: Double
    val seed: Long
}

data class Settings(
    override val outputFile: File,
    override val random: Random,
    override val deltaT: BigDecimal,
    override val mass: BigDecimal,
    override val k: Double,
    override val gamma: Double,
    override val simulationTime: BigDecimal,
    override val initialPositions: List<BigDecimal>,
    override val initialVelocities: List<BigDecimal>,
    override val amplitude: Double,
    override val seed: Long
) : SimulationSettings

data class CoupledSettings(
    val basicSettings: Settings,
    val numberOfParticles: Int,
    val angularFrequency: Double,
    val springLength: Double
) : SimulationSettings by basicSettings {
    // We delegate most properties to basicSettings
    // but can add coupled-specific behavior here

    override val initialPositions: List<BigDecimal>
        get() = basicSettings.initialPositions.take(numberOfParticles)

    override val initialVelocities: List<BigDecimal>
        get() = basicSettings.initialVelocities.take(numberOfParticles)
}