package ar.edu.itba.ss.commands

import ar.edu.itba.ss.integrables.*
import ar.edu.itba.ss.simulation.*
import ar.edu.itba.ss.utils.OutputWriter
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

        val coroutineScope = CoroutineScope(Dispatchers.Default)

        val simulationJobs = mutableListOf<CoupledSimulationJob>()
        runBlocking {
            //val euler = initializeEuler(coroutineScope)
            //simulationJobs.add(euler)

            //val verlet = initializeVerlet(coroutineScope)
            //simulationJobs.add(verlet)

            val beeman = initializeBeeman(coroutineScope)
            simulationJobs.add(beeman)

            //val gear = initializeGearPredictorCorrector(coroutineScope)
            //simulationJobs.add(gear)

            simulationJobs.forEach { it.jobParams.simulationJob.join() }
            logger.info { "All simulations finished. Waiting for writer to finish." }

            simulationJobs.forEach { it.jobParams.writer.requestStop() }
            simulationJobs.forEach { it.jobParams.writerJob.join() }
            simulationJobs.forEach { it.jobParams.output.close() }

            logger.info { "Simulations completed." }
            logger.info { "Outputs saved to:" }
            simulationJobs.forEach { logger.info { "\t${it.settings.basicSettings.outputFile}" } }
        }
    }

    private fun initializeGearPredictorCorrector(scope: CoroutineScope): CoupledSimulationJob {
        val settings = buildCoupledSettings(GearPredictorCorrector.PRETTY_NAME)
        return initializeCoupledAlgorithm(
            settings = settings,
            algorithm = GearPredictorCorrector(settings.basicSettings, Simulation.Companion::calculateAcceleration),
            scope = scope
        )
    }

    private fun initializeBeeman(scope: CoroutineScope): CoupledSimulationJob {
        val settings = buildCoupledSettings(Beeman.PRETTY_NAME)
        return initializeCoupledAlgorithm(
            settings = settings,
            algorithm = Beeman(settings, Simulation.Companion::calculateAcceleration),
            scope = scope
        )
    }

    private fun initializeVerlet(scope: CoroutineScope): CoupledSimulationJob {
        val settings = buildCoupledSettings(Verlet.PRETTY_NAME)
        return initializeCoupledAlgorithm(
            settings = settings,
            algorithm = Verlet(settings, Simulation.Companion::calculateAcceleration),
            scope = scope
        )
    }

    private fun initializeEuler(scope: CoroutineScope): CoupledSimulationJob {
        val settings = buildCoupledSettings(Euler.PRETTY_NAME)
        return initializeCoupledAlgorithm(
            settings = settings,
            algorithm = Euler(
                settings = settings,
                acceleration = Simulation.Companion::calculateAcceleration,
                deltaT = settings.basicSettings.deltaT
            ),
            scope = scope
        )
    }

    private fun buildCoupledFileName(algorithmName: String): String =
        buildString {
            append(algorithmName)
            append("_N=$numberOfParticles")
            append("_w=$angularFrequency")
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

    private fun buildCoupledSettings(algorithmName: String): CoupledSettings {
        val basicSettings = buildBasicSettings(algorithmName)
        return CoupledSettings(
            basicSettings = basicSettings,
            //Not consider the driver particle
            numberOfParticles = numberOfParticles-1,
            angularFrequency = angularFrequency,
            springLength = springLength
        )
    }

    private fun buildBasicSettings(algorithmName: String): Settings {
        val fileName = buildCoupledFileName(algorithmName)
        val outputCsv = outputDirectory.resolve(fileName).toFile()
        return Settings(
            outputFile = outputCsv,
            random = Random(seed),
            deltaT = BigDecimal.valueOf(deltaT),
            mass = BigDecimal.valueOf(mass),
            k = springConstant,
            gamma = gamma,
            simulationTime = BigDecimal.valueOf(finalTime),
            initialPositions = List(numberOfParticles) { BigDecimal.ZERO },  // Default initial positions
            initialVelocities = List(numberOfParticles) { BigDecimal.ZERO }, // Default initial velocities
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
