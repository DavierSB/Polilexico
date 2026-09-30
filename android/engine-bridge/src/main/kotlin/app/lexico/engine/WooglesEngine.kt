package app.lexico.engine

import android.content.Context
import java.io.File
import app.lexico.go.demo.Demo
import app.lexico.go.engine.Engine

object WooglesEngine {
  @Volatile
  private var ready = false

  @Synchronized
  fun start(ctx: Context) {
    if (ready) return
    val data = copyData(ctx)
    Engine.init(data.absolutePath, File(ctx.filesDir, "games").absolutePath)
    ready = true
  }

  fun isValidWord(word: String): Boolean = Engine.isValidWord(word)

  fun bestMoves(board: String, rack: String, n: Int = 15): String =
    Engine.bestMoves(board, rack, n.toLong())

  fun demoGame(): String = Demo.play()

  fun demoGameWithScores(): String = Demo.playWithScores()

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

  private fun copyAssets(ctx: Context, path: String, dst: File) {
    val children = ctx.assets.list(path).orEmpty()
    if (children.isNotEmpty()) return children.forEach { copyAssets(ctx, "$path/$it", File(dst, it)) }
    dst.parentFile?.mkdirs()
    ctx.assets.open(path).use { input -> dst.outputStream().use { input.copyTo(it) } }
  }
}
