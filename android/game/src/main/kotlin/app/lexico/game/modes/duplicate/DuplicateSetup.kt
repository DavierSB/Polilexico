package app.lexico.game.modes.duplicate

data class DuplicateSetup(val invalidLosesTurn: Boolean, val turnMs: Long, val maxRounds: Int = 0, val revealAtEnd: Boolean = false)
