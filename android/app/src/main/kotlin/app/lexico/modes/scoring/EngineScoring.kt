package app.lexico.modes.scoring

import app.lexico.game.Lexico
import app.lexico.ui.board.PlayScorer

/** Los puntos de la jugada que se va colocando, segun el motor. */
fun engineScorer(lexico: Lexico): PlayScorer = PlayScorer(lexico.scorer::score)
