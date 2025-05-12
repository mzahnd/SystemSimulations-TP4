package ar.edu.itba.ss

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.nio.file.Path
import kotlin.random.Random

class DampedOscillatorCommand : CliktCommand(name = "damped") {
    private val logger = KotlinLogging.logger {}

    private val mass: Double by option("-m", "--mass")
        .double()
        .default(70.0)
        .help("Mass [kg]")
        .check("Must be greater than 0") { it > 0.0 }

    private val springConstant: Long by option("-k", "--spring-constant")
        .long()
        .help("Spring constant k [N/m]")
        .default(10000L)

    private val gamma: Long by option("-y", "--gamma")
        .long()
        .help("Gamma kg/s")
        .default(100)

    private val finalTime: Double by option("-t", "--tf", "--simulation-time")
        .double()
        .default(5.0)
        .help("Total simulation time [s]")
        .check("Must be greater than 0") { it > 0.0 }


    private val initialPosition: Double by option("-r", "--r0", "--initial-position")
        .double()
        .default(1.0)
        .help("r(t=0) [m]")
        .check("Must be non-negative") { it >= 0.0 }

    private val amplitude: Long by option("-A", "--amplitude")
        .long()
        .required()
        .help("A")
        .check("Must be greater than zero") { it >= 0 }

    private val initialVelocity: Double? by option("-v", "--v0", "--initial-velocity")
        .double()
        .help("Initial velocity of the particles [m/s]")
        .check("Must be non-negative") { it >= 0.0 }

    private val deltaT: Double by option("-dt", "--deltaT")
        .double()
        .default(1.0)
        .help("dT [s]")
        .check("Must be greater than 0") { it > 0.0 }

    private val seed: Long by option("-s", "--seed").long().default(System.currentTimeMillis())
        .help("[Optional] Seed for the RND").check("Seed must be greater or equal to 0.") { it > 0 }

    private val outputDirectory: Path by option().path(
        canBeFile = false, canBeDir = true, mustExist = true, mustBeReadable = true, mustBeWritable = true
    ).required().help("Path to the output directory.")

    val calculatedInitialVelocity: BigDecimal by lazy {
        if (initialVelocity != null) {
            BigDecimal.valueOf(initialVelocity!!)
        } else {
            BigDecimal.valueOf(-amplitude * gamma / (2 * mass))
        }
    }

    override fun run() {

        logger.info { "Starting simulation with the following parameters:" }
        logger.info { "Particle mass: $mass [kg]" }
        logger.info { "Spring constant: $springConstant [kg]" }
        logger.info { "Gamma: $gamma [kg/s]" }
        logger.info { "Final time: $finalTime [s]" }
        logger.info { "Initial position: $initialPosition [m]" }
        logger.info { "Initial velocity: $calculatedInitialVelocity [m/s]" }
        logger.info { "Seed: $seed" }


        val coroutineScope = CoroutineScope(Dispatchers.Default)

        val simulationJobs = mutableListOf<SimulationJob>()
        runBlocking {
            val euler = initializeEuler(coroutineScope)
            simulationJobs.add(euler)

            val verlet = initializeVerlet(coroutineScope)
            simulationJobs.add(verlet)

            val beeman = initializeBeeman(coroutineScope)
            simulationJobs.add(beeman)

            val gear = initializeGearPredictorCorrector(coroutineScope)
            simulationJobs.add(gear)

            simulationJobs.forEach { it.simulationJob.join() }
            logger.info { "All simulations finished. Waiting for writer to finish." }

            simulationJobs.forEach { it.writer.requestStop() }
            simulationJobs.forEach { it.writerJob.join() }
            simulationJobs.forEach { it.output.close() }

            logger.info { "Simulations completed." }
            logger.info { "Outputs saved to:" }
            simulationJobs.forEach { logger.info { "\t${it.settings.outputFile}" } }
        }
    }

    private fun initializeGearPredictorCorrector(scope: CoroutineScope): SimulationJob {
        val settings = buildSettings(GearPredictorCorrector.PRETTY_NAME)
        return initializeAlgorithm(
            settings = settings,
            algorithm = GearPredictorCorrector(settings, Simulation::calculateAcceleration),
            scope = scope,
        )
    }

    private fun initializeBeeman(scope: CoroutineScope): SimulationJob {
        val settings = buildSettings(Beeman.PRETTY_NAME)
        return initializeAlgorithm(
            settings = settings,
            algorithm = Beeman(settings, Simulation::calculateAcceleration),
            scope = scope,
        )
    }

    private fun initializeVerlet(scope: CoroutineScope): SimulationJob {
        val settings = buildSettings(Verlet.PRETTY_NAME)
        return initializeAlgorithm(
            settings = settings,
            algorithm = Verlet(
                settings, Simulation::calculateAcceleration
            ),
            scope = scope,
        )
    }

    private fun initializeEuler(scope: CoroutineScope): SimulationJob {
        val settings = buildSettings(Euler.PRETTY_NAME)
        return initializeAlgorithm(
            settings = settings,
            algorithm = Euler(
                settings = settings,
                acceleration = Simulation::calculateAcceleration,
                deltaT = settings.deltaT,
            ),
            scope = scope,
        )
    }

    private fun buildFileName(algorithmName: String): String =
        buildString {
            append(algorithmName)
            append("_dT=$deltaT")
            append("_mass=$mass")
            append("_k=$springConstant")
            append("_y=$gamma")
            append("_t=$finalTime")
            append("_r0=$initialPosition")
            append("_v0=$calculatedInitialVelocity")
            append("_A=$amplitude")
            append("_seed=$seed")
        }.replace(".", "_")
            .replace("=", "-")
            .replace(" ", "-")
            .plus(".csv")

    private fun buildSettings(algorithmName: String): Settings {
        val fileName = buildFileName(algorithmName)
        val outputCsv = outputDirectory.resolve(fileName).toFile()
        return Settings(
            outputFile = outputCsv,
            random = Random(seed),
            deltaT = BigDecimal.valueOf(deltaT),
            mass = BigDecimal.valueOf(mass),
            k = springConstant,
            gamma = gamma,
            simulationTime = BigDecimal.valueOf(finalTime),
            r0 = BigDecimal.valueOf(initialPosition),
            v0 = calculatedInitialVelocity,
            amplitude = amplitude,
            seed = seed,
        )
    }

    private fun initializeAlgorithm(
        settings: Settings,
        algorithm: Algorithm,
        scope: CoroutineScope
    ): SimulationJob {
        val output = Channel<String>(capacity = Channel.UNLIMITED)
        val writer = OutputWriter(settings = settings, channel = output)
        val simulation = Simulation(settings, output = output, algorithm = algorithm)

        val writerJob = scope.launch { writer.start() }
        val simulationJob = scope.launch { simulation.simulate() }

        return SimulationJob(
            algorithm = algorithm,
            output = output,
            writer = writer,
            writerJob = writerJob,
            simulation = simulation,
            simulationJob = simulationJob,
            settings = settings
        )
    }
}