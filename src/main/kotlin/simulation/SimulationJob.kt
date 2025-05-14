package ar.edu.itba.ss.simulation

import ar.edu.itba.ss.integrables.AlgorithmN
import ar.edu.itba.ss.utils.OutputWriter
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel

data class SimulationJob<T : SimulationSettings>(
    val algorithm: AlgorithmN,
    val output: Channel<String>,
    val writer: OutputWriter,
    val writerJob: Job,
    val simulation: Simulation<T>,
    val simulationJob: Job,
)

data class DampedSimulationJob(
    val jobParams: SimulationJob<Settings>,
    val settings: Settings
)

data class CoupledSimulationJob(
    val jobParams: SimulationJob<CoupledSettings>,
    val settings: CoupledSettings
)