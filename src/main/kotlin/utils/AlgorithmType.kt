package ar.edu.itba.ss.utils

enum class AlgorithmType(val prettyName: String) {
    BEEMAN("Beeman"),
    VERLET("Verlet"),
    EULER("Euler"),
    GEAR("Gear");

    override fun toString() = prettyName
}
