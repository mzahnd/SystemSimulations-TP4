package ar.edu.itba.ss.simulation

import ar.edu.itba.ss.integrables.AlgorithmN
import ch.obermuhlner.math.big.DefaultBigDecimalMath.createLocalMathContext
import ch.obermuhlner.math.big.kotlin.bigdecimal.div
import ch.obermuhlner.math.big.kotlin.bigdecimal.plus
import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class Simulation<T : SimulationSettings>(
    private val settings: T,
    private val output: Channel<String>,
    private val algorithm: AlgorithmN,
    val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val logger = KotlinLogging.logger {}
    private var currentTime = BigDecimal.ZERO

    suspend fun simulate() = withContext(dispatcher) {
        output.send(buildOutputHeader())
        output.send(buildParametersLine())

        // Header
        output.send("time,id,r,v,a\n")

        createLocalMathContext(34).use {
            while (currentTime <= settings.simulationTime) {
                if (settings is CoupledSettings) {
                    settings.updateDrivenParticle(currentTime)
                }
                algorithm.advanceDeltaT()
                currentTime += settings.deltaT
                saveState()
            }
        }

        logger.info { "Finished simulation" }
    }

    private fun buildOutputHeader(): String {
        return when (settings) {
            is CoupledSettings -> "dT,m,k,y,A,N,w,l,seed\n"
            else -> "dT,m,k,y,r0,v0,A,seed\n"
        }
    }

    private fun buildParametersLine(): String {
        return when (settings) {
            is CoupledSettings -> listOf(
                "%.6f".format(settings.basicSettings.deltaT),
                "%.8f".format(settings.basicSettings.mass),
                settings.basicSettings.k,
                settings.basicSettings.gamma,
                settings.basicSettings.amplitude,
                settings.numberOfParticles,
                settings.angularFrequency,
                settings.springLength,
                settings.basicSettings.seed
            )
            else -> listOf(
                "%.6f".format(settings.deltaT),
                "%.8f".format(settings.mass),
                settings.k,
                settings.gamma,
                "%.8f".format(settings.initialPositions[0]),
                "%.8f".format(settings.initialVelocities[0]),
                settings.amplitude,
                settings.seed
            )
        }.joinToString(separator = ",", postfix = "\n")
    }

    private suspend fun saveState() {

        // Save driven particle state if coupled system
        if (settings is CoupledSettings) {
            output.send(
                listOf(
                    currentTime.toPlainString(),
                    "0", // Particle ID
                    settings.currentDrivenPosition.toPlainString(),
                    settings.currentDrivenVelocity.toPlainString(),
                    "0" // Acceleration is 0 for driven particle
                ).joinToString(separator = ",", postfix = "\n")
            )
        }

        algorithm.currentPositions.forEachIndexed { index, position ->
            output.send(
                listOf(
                    currentTime.toPlainString(),
                    index.inc().toString(), // Particle ID
                    position.toPlainString(),
                    algorithm.currentVelocities[index].toPlainString(),
                    algorithm.currentAccelerations[index].toPlainString(),
                ).joinToString(separator = ",", postfix = "\n")
            )
        }
    }

    companion object {
        fun calculateAcceleration(
            settings: SimulationSettings,
            currentPositions: List<BigDecimal>,
            currentVelocities: List<BigDecimal>
        ): List<BigDecimal> {
            require(currentPositions.size == currentVelocities.size) {
                "Positions and velocities must have the same size"
            }

            return when (settings) {
                is CoupledSettings -> calculateCoupledAcceleration(settings, currentPositions, currentVelocities)
                else -> calculateDampedAcceleration(settings, currentPositions, currentVelocities)
            }
        }

        private fun calculateDampedAcceleration(
            settings: SimulationSettings,
            currentPositions: List<BigDecimal>,
            currentVelocities: List<BigDecimal>
        ): List<BigDecimal> {
            return currentPositions.zip(currentVelocities) { x, v ->
                ((settings.k * x + settings.gamma * v) / settings.mass) * (-1).toBigDecimal()
            }
        }

        private fun calculateCoupledAcceleration(
            settings: CoupledSettings,
            currentPositions: List<BigDecimal>,
            currentVelocities: List<BigDecimal>
        ): List<BigDecimal> {
            val k = settings.basicSettings.k.toBigDecimal()
            val gamma = settings.basicSettings.gamma.toBigDecimal()
            val mass = settings.basicSettings.mass

            return currentPositions.indices.map { i ->
                // Left neighbor is either driven particle or previous integrated particle
                val leftNeighbor = when (i) {
                    0 -> settings.currentDrivenPosition  // First integrated particle connects to driven
                    else -> currentPositions[i-1]
                }

                // Right neighbor (if exists)
                val rightNeighbor = if (i == currentPositions.lastIndex)
                    null  // Last particle has no right neighbor
                else
                    currentPositions[i+1]

                var force = -k * (currentPositions[i] - leftNeighbor)
                rightNeighbor?.let { force += -k * (currentPositions[i] - it) }
                force += -gamma * currentVelocities[i]

                force / mass
            }
        }
    }
}