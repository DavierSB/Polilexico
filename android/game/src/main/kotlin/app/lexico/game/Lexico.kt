package app.lexico.game

import android.content.Context
import app.lexico.engine.WooglesEngine
import app.lexico.game.analysis.EngineAnalyst
import app.lexico.game.engine.engine
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.classic.ClassicSetup
import app.lexico.game.modes.classic.EndgameSetup
import app.lexico.game.modes.duplicate.DuplicateGame
import app.lexico.game.modes.duplicate.DuplicateSetup
import app.lexico.game.modes.recall.DemoGames
import app.lexico.game.modes.sprint.SprintGame
import app.lexico.game.modes.sprint.SprintSetup
import app.lexico.game.records.FinishedGames
import app.lexico.game.records.RecallRecords
import app.lexico.game.records.SprintRecords
import app.lexico.game.scoring.EngineScorer
import app.lexico.game.storage.Mode
import app.lexico.game.storage.SavedGame
import app.lexico.game.storage.SavedGames
import app.lexico.go.classic.Classic
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class Lexico(private val context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val savedGames = SavedGames(File(context.filesDir, "in_progress"))

  val finishedGames = FinishedGames(File(context.filesDir, "games"))

  val analyst = EngineAnalyst()

  val scorer = EngineScorer()

  val demoGames = DemoGames()

  val sprintRecords = SprintRecords(context)

  val recallRecords = RecallRecords(context)

  suspend fun start() = engine { WooglesEngine.start(context) }

  suspend fun bots(): List<String> = engine { Classic.bots().split(",") }

  suspend fun newClassic(setup: ClassicSetup): ClassicGame = ClassicGame.start(setup, newId(), savedGames, scope)

  suspend fun newEndgame(setup: EndgameSetup): ClassicGame = ClassicGame.startEndgame(setup, newId(), savedGames, scope)

  suspend fun newDuplicate(setup: DuplicateSetup): DuplicateGame = DuplicateGame.start(setup, newId(), savedGames, scope)

  suspend fun newSprint(setup: SprintSetup): SprintGame = SprintGame.start(setup, newId(), savedGames, scope)

  suspend fun continueGame(saved: SavedGame): LiveGame<*> {
    val text = engine { savedGames.read(saved) }
    return when (saved.mode) {
      Mode.CLASSIC, Mode.ENDGAME -> ClassicGame.load(text, saved.id, saved.mode, savedGames, scope)
      Mode.DUPLICATE -> DuplicateGame.load(text, saved.id, savedGames, scope)
    }
  }

  private fun newId(): String = UUID.randomUUID().toString()
}
