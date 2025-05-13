package ar.edu.itba.ss

import ar.edu.itba.ss.commands.Cli
import ar.edu.itba.ss.commands.CoupledOscillatorCommand
import ar.edu.itba.ss.commands.DampedOscillatorCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = Cli()
    .subcommands(DampedOscillatorCommand(), CoupledOscillatorCommand())
    .main(args)