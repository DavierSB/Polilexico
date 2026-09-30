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

/**
 * La puerta de entrada al juego: arranca el motor y crea o continua partidas. Una por app.
 *
 * Las partidas viven en su propio ambito, no en el de una pantalla: al salir de la pantalla se
 * pausan y se guardan ([LiveGame.close]) aunque la pantalla ya no exista.
 */
class Lexico(private val context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /** Las partidas en curso guardadas. */
  val savedGames = SavedGames(File(context.filesDir, "in_progress"))

  /** Las partidas terminadas (los registros que escribe el motor), para revisarlas. */
  val finishedGames = FinishedGames(File(context.filesDir, "games"))

  val analyst = EngineAnalyst()

  /** Los puntos de la jugada que se va colocando. */
  val scorer = EngineScorer()

  val demoGames = DemoGames()

  /** Los records de Scrabble Sprint. */
  val sprintRecords = SprintRecords(context)

  /** Los records de "¿Cuántas recuerdas?". */
  val recallRecords = RecallRecords(context)

  /** Carga el motor y el diccionario; hay que esperarlo antes de lo demas. */
  suspend fun start() = engine { WooglesEngine.start(context) }

  /** Los bots de Woogles, de menor a mayor nivel. */
  suspend fun bots(): List<String> = engine { Classic.bots().split(",") }

  suspend fun newClassic(setup: ClassicSetup): ClassicGame = ClassicGame.start(setup, newId(), savedGames, scope)

  /** Finales: busca un final de HastyBot contra si mismo con la ventaja pedida y te lo da en tu turno. */
  suspend fun newEndgame(setup: EndgameSetup): ClassicGame = ClassicGame.startEndgame(setup, newId(), savedGames, scope)

  suspend fun newDuplicate(setup: DuplicateSetup): DuplicateGame = DuplicateGame.start(setup, newId(), savedGames, scope)

  /** Una serie de Scrabble Sprint; como minijuego, no se guarda. */
  suspend fun newSprint(setup: SprintSetup): SprintGame = SprintGame.start(setup, newId(), savedGames, scope)

  /** Continua una partida guardada; empieza en pausa. */
  suspend fun continueGame(saved: SavedGame): LiveGame<*> {
    val text = engine { savedGames.read(saved) }
    return when (saved.mode) {
      Mode.CLASSIC, Mode.ENDGAME -> ClassicGame.load(text, saved.id, saved.mode, savedGames, scope)
      Mode.DUPLICATE -> DuplicateGame.load(text, saved.id, savedGames, scope)
    }
  }

  private fun newId(): String = UUID.randomUUID().toString()
}
