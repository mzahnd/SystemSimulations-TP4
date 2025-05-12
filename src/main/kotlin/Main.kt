package ar.edu.itba.ss

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = Cli()
    .subcommands(DampedOscillatorCommand(),CoupledOscillatorCommand())
    .main(args)