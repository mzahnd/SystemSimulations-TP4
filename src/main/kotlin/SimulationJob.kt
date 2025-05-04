package ar.edu.itba.ss

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel

data class SimulationJob(
    val algorithm: Algorithm,
    val output: Channel<String>,
    val writer: OutputWriter,
    val writerJob: Job,
    val simulation: Simulation,
    val simulationJob: Job,
    val settings: Settings
)
