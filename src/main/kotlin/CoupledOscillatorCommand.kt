package ar.edu.itba.ss

import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import io.github.oshai.kotlinlogging.KotlinLogging

class CoupledOscillatorCommand : OscillatorCommand(){
    private val logger = KotlinLogging.logger {}

    private val numberOfParticles: Int by option("-N", "--number-of-particles")
        .int()
        .default(1000)
        .help("N - Number of particles")
        .check("Must be greater than zero") {it > 0}

    private val angularFrequency: Double? by option("-w", "--angular-frequency")
        .double()
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
    }
}