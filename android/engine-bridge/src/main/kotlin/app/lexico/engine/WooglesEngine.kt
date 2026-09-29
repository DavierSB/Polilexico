package app.lexico.engine

import android.content.Context
import java.io.File
import app.lexico.go.demo.Demo
import app.lexico.go.engine.Engine

/**
 * Puerta de entrada al motor de Woogles (macondo, en Go). Hay que llamar a [start] una vez,
 * fuera del hilo principal, antes de crear partidas.
 */
object WooglesEngine {
  @Volatile
  private var ready = false

  /** Copia el diccionario de los assets de este modulo y carga el motor. */
  @Synchronized
  fun start(ctx: Context) {
    if (ready) return
    val data = copyData(ctx)
    Engine.init(data.absolutePath, File(ctx.filesDir, "games").absolutePath)
    ready = true
  }

  fun isValidWord(word: String): Boolean = Engine.isValidWord(word)

  /**
   * El "gen" del shell de macondo sobre una posicion cualquiera: `board` son las 225 casillas
   * (fila A..O, columna 1..15) separadas por espacios, "." vacia y minuscula = comodin; `rack`
   * como "AEIRST?" ([CH] para los digrafos). Devuelve las `n` mejores jugadas segun HastyBot en
   * JSON, con el mismo formato que las candidatas del -log.json de vsbot.
   */
  fun bestMoves(board: String, rack: String, n: Int = 15): String =
    Engine.bestMoves(board, rack, n.toLong())

  /** Una partida entera de HastyBot contra si mismo (sus colocaciones, en JSON), de adorno. */
  fun demoGame(): String = Demo.play()

  /**
   * Como [demoGame], con los puntos de cada colocacion: `[{"placement":"H8 CA.A","score":12}]`.
   * Es la partida a recordar de "¿Cuántas recuerdas?".
   */
  fun demoGameWithScores(): String = Demo.playWithScores()

  /** Copia assets/data a filesDir una vez por instalacion (se rehace al actualizar la app). */
  private fun copyData(ctx: Context): File {
    val target = File(ctx.filesDir, "data")
    val marker = File(target, ".version")
    val version = ctx.packageManager.getPackageInfo(ctx.packageName, 0).lastUpdateTime.toString()
    if (marker.exists() && marker.readText() == version) return target
    target.deleteRecursively()
    copyAssets(ctx, "data", target)
    marker.writeText(version)
    return target
  }

  /** Copia la carpeta (o el archivo) `path` de los assets a `dst`, recursivamente. */
  private fun copyAssets(ctx: Context, path: String, dst: File) {
    val children = ctx.assets.list(path).orEmpty()
    if (children.isNotEmpty()) return children.forEach { copyAssets(ctx, "$path/$it", File(dst, it)) }
    dst.parentFile?.mkdirs()
    ctx.assets.open(path).use { input -> dst.outputStream().use { input.copyTo(it) } }
  }
}
