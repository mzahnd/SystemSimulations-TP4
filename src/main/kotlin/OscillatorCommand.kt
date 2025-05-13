package ar.edu.itba.ss

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

abstract class OscillatorCommand : CliktCommand() {
     protected val mass: Double by option("-m", "--mass")
        .double()
        .default(70.0)
        .help("Mass [kg]")
        .check("Must be greater than 0") { it > 0.0 }

    protected val springConstant: Double by option("-k", "--spring-constant")
        .double()
        .help("Spring constant k [N/m]")
        .default(10000.0)

    protected val gamma: Double by option("-y", "--gamma")
        .double()
        .help("Gamma kg/s")
        .default(100.0)

    protected val finalTime: Double by option("-t", "--tf", "--simulation-time")
        .double()
        .default(5.0)
        .help("Total simulation time [s]")
        .check("Must be greater than 0") { it > 0.0 }

    protected val deltaT: Double by option("-dt", "--deltaT")
        .double()
        .default(1.0)
        .help("dT [s]")
        .check("Must be greater than 0") { it > 0.0 }

    protected val amplitude: Double by option("-A", "--amplitude")
        .double()
        .default(1.0)
        .help("A")
        .check("Must be greater than zero") { it >= 0 }

    protected val seed: Long by option("-s", "--seed").long().default(System.currentTimeMillis())
        .help("[Optional] Seed for the RND").check("Seed must be greater or equal to 0.") { it > 0 }

    protected val outputDirectory: Path by option().path(
        canBeFile = false, canBeDir = true, mustExist = true, mustBeReadable = true, mustBeWritable = true
    ).required().help("Path to the output directory.")
}