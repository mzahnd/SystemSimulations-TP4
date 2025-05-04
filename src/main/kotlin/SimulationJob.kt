package ar.edu.itba.ss

import kotlinx.coroutines.channels.Channel

data class SimulationJob(
    val algorithm: Algorithm,
    val output: Channel<String>,
    val writer: OutputWriter,
    val simulation: Simulation,
    val settings: Settings
)
