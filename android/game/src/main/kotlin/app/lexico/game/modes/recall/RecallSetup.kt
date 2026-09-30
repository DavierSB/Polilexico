package app.lexico.game.modes.recall

/**
 * Las opciones de una serie de "¿Cuántas recuerdas?" que cuentan para su record: la pausa entre
 * jugadas, las palabras de cada partida, las vidas y void (false) o single (true).
 */
data class RecallSetup(val intervalMs: Long, val wordsPerGame: Int, val lives: Int, val single: Boolean)
