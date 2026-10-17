package app.lexico.update

internal fun isNewer(candidate: String, current: String): Boolean {
  val a = parts(candidate)
  val b = parts(current)
  val diff = (0 until maxOf(a.size, b.size)).map { a.getOrElse(it) { 0 } - b.getOrElse(it) { 0 } }.firstOrNull { it != 0 }
  return (diff ?: 0) > 0
}

private fun parts(version: String): List<Int> = version.trim().split('.').map { it.toIntOrNull() ?: 0 }
