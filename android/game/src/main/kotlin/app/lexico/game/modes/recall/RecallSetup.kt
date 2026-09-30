package app.lexico.game.modes.recall

data class RecallSetup(val intervalMs: Long, val wordsPerGame: Int, val lives: Int, val single: Boolean)
