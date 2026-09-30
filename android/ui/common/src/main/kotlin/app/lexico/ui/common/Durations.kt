package app.lexico.ui.common

object Durations {
  fun format(ms: Long): String {
    val s = (ms.coerceAtLeast(0) + 999) / 1000
    return "${s / 60}:${"%02d".format(s % 60)}"
  }

  fun parse(text: String): Long? {
    val parts = text.trim().split(":")
    val nums = parts.map { it.trim().toLongOrNull() ?: return null }
    return when (nums.size) {
      1 -> nums[0] * 60_000
      2 -> if (nums[1] in 0..59) (nums[0] * 60 + nums[1]) * 1000 else null
      else -> null
    }?.takeIf { it >= 0 }
  }
}
