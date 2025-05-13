package ar.edu.itba.ss.simulation

import ar.edu.itba.ss.integrables.AlgorithmN
import ar.edu.itba.ss.utils.OutputWriter
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel

data class SimulationJob(
    val algorithm: AlgorithmN,
    val output: Channel<String>,
    val writer: OutputWriter,
    val writerJob: Job,
    val simulation: Simulation,
    val simulationJob: Job,
    val settings: Settings
)
