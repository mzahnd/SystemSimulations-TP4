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

class Simulation(
    private val settings: Settings,
    private val output: Channel<String>,
    private val algorithm: AlgorithmN,
    val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val logger = KotlinLogging.logger {}

    private var currentTime = BigDecimal.ZERO

    suspend fun simulate() = withContext(dispatcher) {
        // Params
        output.send("dT,m,k,y,r0,v0,A,seed\n")
        output.send(
            listOf(
                "%.6f".format(settings.deltaT),
                "%.8f".format(settings.mass),
                settings.k,
                settings.gamma,
                "%.8f".format(settings.initialPositions[0]),
                "%.8f".format(settings.initialVelocities[0]),
                settings.amplitude,
                settings.seed,
            ).joinToString(separator = ",", postfix = "\n")
        )

        // Header
        output.send("time,id,r,v,a\n")

        createLocalMathContext(34).use {
            while (currentTime <= settings.simulationTime) {
                algorithm.advanceDeltaT() // parameter always ignored
                currentTime += settings.deltaT
                saveState()
            }
        }

        logger.info { "Finished simulation" }
    }

    private suspend fun saveState() {
        algorithm.currentPositions.forEachIndexed { index, position ->
            output.send(
                listOf(
                    currentTime.toPlainString(),
                    index.toString(), // Particle ID
                    position.toPlainString(),
                    algorithm.currentVelocities[index].toPlainString(),
                    algorithm.currentAccelerations[index].toPlainString(),
                ).joinToString(separator = ",", postfix = "\n")
            )
        }
    }

    companion object {
        fun calculateAcceleration(
            settings: Settings,
            currentPositions: List<BigDecimal>,
            currentVelocities: List<BigDecimal>
        ): List<BigDecimal> {
            require(currentPositions.size == currentVelocities.size) {
                "Positions and velocities must have the same size"
            }
            return currentPositions.zip(currentVelocities) { x, v ->
                ((settings.k * x + settings.gamma * v) / settings.mass) * (-1).toBigDecimal()
            }
        }
    }
}