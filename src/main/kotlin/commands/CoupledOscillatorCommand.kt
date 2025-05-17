package ar.edu.itba.ss.commands

import ar.edu.itba.ss.integrables.*
import ar.edu.itba.ss.simulation.*
import ar.edu.itba.ss.utils.AlgorithmType
import ar.edu.itba.ss.utils.OutputWriter
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.math.BigDecimal
import kotlin.random.Random

class CoupledOscillatorCommand : OscillatorCommand() {
    private val logger = KotlinLogging.logger {}

    private val numberOfParticles: Int by option("-N", "--number-of-particles")
        .int()
        .default(1000)
        .help("N - Number of particles")
        .check("Must be greater than zero") { it > 0 }

    private val angularFrequency: Double by option("-w", "--angular-frequency")
        .double()
        .default(1.0)
        .help("w [rad/s]")

    private val springLength: Double by option("-l", "--spring-length")
        .double()
        .default(0.001)
        .help("l [m]")

    private val algorithmType: AlgorithmType by option("-a", "--algorithm")
        .enum<AlgorithmType> { it.name.lowercase() }
        .default(AlgorithmType.BEEMAN)

    override fun run() {
        logger.info { "Starting simulation with the following parameters:" }
        logger.info { "Particle mass: $mass [kg]" }
        logger.info { "Spring constant: $springConstant [kg]" }
        logger.info { "Gamma: $gamma [kg/s]" }
        logger.info { "Final time: $finalTime [s]" }
        logger.info { "Number of particles: $numberOfParticles" }
        logger.info { "Angular frequency: $angularFrequency [rad/s]" }
        logger.info { "Spring length: $springLength [m]" }
        logger.info { "Seed: $seed" }

        val angularFrequencies = listOf(
            1.0, 1.105, 1.211, 1.316, 1.421,
            1.526, 1.632, 1.737, 1.842, 1.947,
            2.053, 2.158, 2.263, 2.368, 2.474,
            2.579, 2.684, 2.789, 2.895, 3.0
        )

        runBlocking {
            val coroutineScope = this

            val simulationJobs = angularFrequencies.map { omega ->
                launch {
                    val job = initializeWithFrequency(
                        scope = coroutineScope,
                        omega = omega,
                        algorithmName = algorithmType.prettyName,
                        algorithmFactory = ::algorithmFactory
                    )
                    job.jobParams.simulationJob.join()
                    job.jobParams.writer.requestStop()
                    job.jobParams.writerJob.join()
                    job.jobParams.output.close()

                    logger.info { "Simulation with w=$omega completed. Output: ${job.settings.basicSettings.outputFile}" }
                }
            }

            simulationJobs.joinAll()
            logger.info { "All simulations completed." }
        }
    }

    private fun algorithmFactory(settings: CoupledSettings): AlgorithmN {
        return when (algorithmType) {
            AlgorithmType.BEEMAN -> Beeman(settings, Simulation.Companion::calculateAcceleration)
            AlgorithmType.VERLET -> Verlet(settings, Simulation.Companion::calculateAcceleration)
            AlgorithmType.EULER -> Euler(
                settings = settings,
                acceleration = Simulation.Companion::calculateAcceleration,
                deltaT = settings.deltaT
            )
            AlgorithmType.GEAR -> GearPredictorCorrector(settings, Simulation.Companion::calculateAcceleration)
        }
    }

    private fun initializeWithFrequency(
        scope: CoroutineScope,
        omega: Double,
        algorithmName: String,
        algorithmFactory: (CoupledSettings) -> AlgorithmN
    ): CoupledSimulationJob {
        val settings = buildCoupledSettings(algorithmName, omega)
        return initializeCoupledAlgorithm(
            settings = settings,
            algorithm = algorithmFactory(settings),
            scope = scope
        )
    }

    private fun buildCoupledFileName(algorithmName: String, omega: Double): String =
        buildString {
            append(algorithmName)
            append("_N=$numberOfParticles")
            append("_w=$omega")
            append("_l=$springLength")
            append("_dT=$deltaT")
            append("_mass=$mass")
            append("_k=$springConstant")
            append("_y=$gamma")
            append("_A=$amplitude")
            append("_t=$finalTime")
            append("_seed=$seed")
        }.replace(".", "_")
            .replace("=", "-")
            .replace(" ", "-")
            .plus(".csv")

    private fun buildCoupledSettings(algorithmName: String, omega: Double = angularFrequency): CoupledSettings {
        val basicSettings = buildBasicSettings(algorithmName, omega)
        return CoupledSettings(
            basicSettings = basicSettings,
            numberOfParticles = numberOfParticles - 1,
            angularFrequency = omega,
            springLength = springLength
        )
    }

    private fun buildBasicSettings(algorithmName: String, omega: Double): Settings {
        val fileName = buildCoupledFileName(algorithmName, omega)
        val outputCsv = outputDirectory.resolve(fileName).toFile()
        return Settings(
            outputFile = outputCsv,
            random = Random(seed),
            deltaT = BigDecimal.valueOf(deltaT),
            mass = BigDecimal.valueOf(mass),
            k = springConstant,
            gamma = gamma,
            simulationTime = BigDecimal.valueOf(finalTime),
            initialPositions = List(numberOfParticles) { BigDecimal.ZERO },
            initialVelocities = List(numberOfParticles) { BigDecimal.ZERO },
            amplitude = amplitude,
            seed = seed
        )
    }
    
    private fun initializeCoupledAlgorithm(
        settings: CoupledSettings,
        algorithm: AlgorithmN,
        scope: CoroutineScope
    ): CoupledSimulationJob {
        val output = Channel<String>(capacity = Channel.UNLIMITED)
        val writer = OutputWriter(settings = settings.basicSettings, channel = output)
        val simulation = Simulation(settings, output = output, algorithm = algorithm)

        val writerJob = scope.launch { writer.start() }
        val simulationJob = scope.launch { simulation.simulate() }

        val baseJob = SimulationJob(
            algorithm = algorithm,
            output = output,
            writer = writer,
            writerJob = writerJob,
            simulation = simulation,
            simulationJob = simulationJob
        )

        return CoupledSimulationJob(
            jobParams = baseJob,
            settings = settings
        )
    }
}
