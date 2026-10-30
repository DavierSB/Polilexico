package app.lexico.wifi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import java.util.concurrent.Executors

private const val PING_MS = 3_000L
private const val SILENCE_MS = 10_000

internal class Link(private val socket: Socket) {
  private val reader: BufferedReader = socket.getInputStream().bufferedReader()
  private val writer: BufferedWriter = socket.getOutputStream().bufferedWriter()
  private val outbox = Executors.newSingleThreadExecutor()

  fun run(scope: CoroutineScope, onWire: suspend (Link, Wire) -> Unit, onDrop: (Link) -> Unit) {
    scope.launch(Dispatchers.IO) {
      socket.soTimeout = SILENCE_MS
      val pinger = launch { pingWhileOpen() }
      runCatching { readWhileOpen(onWire) }
      pinger.cancel()
      close()
      onDrop(this@Link)
    }
  }

  fun send(wire: Wire) {
    runCatching { outbox.execute { runCatching { write(wire.encode()) }.onFailure { close() } } }
  }

  fun close() {
    runCatching { socket.close() }
    outbox.shutdown()
  }

  private suspend fun readWhileOpen(onWire: suspend (Link, Wire) -> Unit) {
    while (true) {
      val line = reader.readLine() ?: return
      decode(line)?.takeIf { it != Wire.Ping }?.let { onWire(this, it) }
    }
  }

  private suspend fun CoroutineScope.pingWhileOpen() {
    while (isActive) {
      send(Wire.Ping)
      delay(PING_MS)
    }
  }

  @Synchronized
  private fun write(line: String) {
    writer.write(line)
    writer.newLine()
    writer.flush()
  }
}
