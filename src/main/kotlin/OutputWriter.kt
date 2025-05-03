package ar.edu.itba.ss

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean

class OutputWriter(
    private val settings: Settings,
    private val channel: Channel<String>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private var running = AtomicBoolean(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun start() = withContext(dispatcher) {
        running.set(true)
        val writer = settings.outputFile.bufferedWriter()
        while (running.get() || !channel.isEmpty) {
            val toWrite = channel.tryReceive().getOrNull() ?: continue
            writer.write(toWrite)
            yield()
        }

        writer.close()
    }

    fun requestStop() {
        running.set(false)
    }
}