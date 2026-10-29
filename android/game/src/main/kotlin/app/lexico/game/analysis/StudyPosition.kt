package app.lexico.game.analysis

import app.lexico.model.Board

data class StudyPosition(val board: Board, val rack: List<String>, val myScore: Int = 0, val opponentScore: Int = 0)
