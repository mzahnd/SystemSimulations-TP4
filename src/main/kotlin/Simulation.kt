package ar.edu.itba.ss

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
import java.math.RoundingMode

class Simulation(
    private val settings: Settings,
    private val output: Channel<String>,
    private val algorithm: Algorithm,
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
                "%.8f".format(settings.r0),
                "%.8f".format(settings.v0),
                settings.amplitude,
                settings.seed,
            ).joinToString(separator = ",", postfix = "\n")
        )

        // Header
        output.send("time,r,v,a\n")

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
        output.send(
            listOf(
                currentTime.toPlainString(),
                algorithm.currentPosition.toPlainString(),
                algorithm.currentVelocity.toPlainString(),
                algorithm.currentAcceleration.toPlainString(),
            ).joinToString(separator = ",", postfix = "\n")
        )
    }

    companion object {
        fun calculateAcceleration(
            settings: Settings,
            currentPosition: BigDecimal,
            currentVelocity: BigDecimal
        ): BigDecimal =
            ((settings.k * currentPosition + settings.gamma * currentVelocity) / settings.mass) * -1
    }
}