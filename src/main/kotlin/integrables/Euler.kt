package ar.edu.itba.ss.integrables

import ar.edu.itba.ss.simulation.SimulationSettings
import ch.obermuhlner.math.big.kotlin.bigdecimal.times
import java.math.BigDecimal

class Euler<T : SimulationSettings>(
    val settings: T,
    val acceleration: (settings: T, positions: List<BigDecimal>, velocities: List<BigDecimal>) -> List<BigDecimal>,
    deltaT: BigDecimal
) : AlgorithmN {
    private val dT = deltaT

    override var currentVelocities: List<BigDecimal> = settings.initialVelocities
        private set
    override var currentPositions: List<BigDecimal> = settings.initialPositions
        private set
    override var currentAccelerations: List<BigDecimal> = acceleration(settings, currentPositions, currentVelocities)
        private set

    override fun advanceDeltaT() {
        val r0 = currentPositions
        val v0 = currentVelocities
        val a0 = currentAccelerations

        val v1 = v0.indices.map { i -> v0[i] + dT * a0[i] }
        val r1 = r0.indices.map { i -> r0[i] + dT * v1[i] }

        currentVelocities = v1
        currentPositions = r1
        currentAccelerations = acceleration(settings, r1, v1)
    }

    companion object {
        const val PRETTY_NAME = "Euler"
    }
}