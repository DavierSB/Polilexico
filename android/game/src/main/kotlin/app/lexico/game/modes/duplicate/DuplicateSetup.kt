package app.lexico.game.modes.duplicate

/** Como sera una duplicada: void o single (ver ClassicSetup) y el tiempo por turno. */
data class DuplicateSetup(val invalidLosesTurn: Boolean, val turnMs: Long)
