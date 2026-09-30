package app.lexico.ui.recall

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** De donde salen las partidas a recordar (el motor, o datos de ejemplo). */
interface GameSource {
  /** Una partida entera, colocacion a colocacion. Puede tardar: se llama fuera del hilo principal. */
  suspend fun game(): List<ScoredPlacement>

  /**
   * Si las fichas juntas son una palabra valida (digrafos entre corchetes: "[CH]E"). Decide que
   * fichas fijas pueden ir pegadas y si vale lo que arma el jugador. Tambien fuera del hilo principal.
   */
  fun isWord(word: String): Boolean = false
}

/** Donde se guarda el record de la serie (palabras recordadas) con sus opciones. */
interface RecordBook {
  /** El record; 0 si aun no hay. */
  fun best(): Int

  /** Anota una serie terminada con `recalled` palabras. */
  fun submit(recalled: Int): Record
}

/** El record con las opciones de la serie, y si esta lo acaba de batir. */
data class Record(val best: Int, val isNew: Boolean)

/** Una partida con sus palabras a recordar ya elegidas. */
private class RoundGame(val game: List<ScoredPlacement>, val words: List<WordToRecall>)

/** En que punto de la partida esta la serie. */
sealed interface Stage {
  /** Esperando la partida (solo si el motor tarda mas que la partida anterior). */
  data object Loading : Stage

  /** Se esta viendo la partida. */
  data class Watching(val game: List<ScoredPlacement>) : Stage

  /** Armando la palabra `index` de la partida; `verdict`, en cuanto queda resuelta. */
  data class Solving(val index: Int, val verdict: Verdict? = null) : Stage

  /** Partida terminada con vidas: sus aciertos, y a por la siguiente. */
  data object RoundDone : Stage

  /** Sin vidas: el total, el record y la pregunta de si jugar otra. */
  data object SeriesDone : Stage

  /** No se pudo conseguir la partida; se puede reintentar. */
  data class Failed(val message: String) : Stage
}

/** Como quedo una palabra. Todo menos [HIT] cuesta una vida. */
enum class Verdict {
  /** Era esa. */
  HIT,

  /** No es una palabra valida (en single; en void se rechaza y se corrige). */
  INVALID,

  /** Es valida, pero no la que se jugo. */
  NOT_PLAYED,

  /** "No la recuerdo". */
  FORGOTTEN,
}

/** Lo que pasa al enviar una palabra: queda resuelta, o (en void) se rechaza por no ser valida. */
sealed interface Submission {
  data class Resolved(val verdict: Verdict) : Submission

  data object Rejected : Submission
}

/** Una palabra ya resuelta: `hit` = era esa. */
data class Answer(val word: WordToRecall, val hit: Boolean)

/**
 * Una serie de "¿Cuántas recuerdas?": partida tras partida de la [source], hasta quedarse sin
 * vidas; cada palabra no recordada cuesta una. Mientras se juega una partida ya se pide la
 * siguiente, para no esperar al motor.
 */
@Stable
class RecallSession(
  val config: RecallConfig,
  private val source: GameSource,
  private val records: RecordBook,
  private val scope: CoroutineScope,
  private val random: Random = Random.Default,
) {
  var stage: Stage by mutableStateOf(Stage.Loading)
    private set

  /** Las palabras a recordar de esta partida, en el orden en que se preguntan. */
  var words: List<WordToRecall> by mutableStateOf(emptyList())
    private set

  /** Lo resuelto en esta partida. */
  var answers: List<Answer> by mutableStateOf(emptyList())
    private set

  var lives: Int by mutableStateOf(config.lives)
    private set

  /** Palabras recordadas en la serie. */
  var recalled: Int by mutableStateOf(0)
    private set

  /** Palabras preguntadas en la serie. */
  var asked: Int by mutableStateOf(0)
    private set

  /** El record a batir: el de antes de empezar la serie. */
  var best: Int by mutableStateOf(records.best())
    private set

  /** Al terminar la serie, su record; null mientras sigue. */
  var record: Record? by mutableStateOf(null)
    private set

  /** Cuantas partidas van, desde 1. */
  var round: Int by mutableStateOf(1)
    private set

  private var next: Deferred<Result<RoundGame>> = fetch()

  init {
    startRound()
  }

  /** La partida se termino de ver: a armar sus palabras. */
  fun watched() {
    if (stage !is Stage.Watching) return
    stage = if (words.isEmpty()) endRound() else Stage.Solving(0)
  }

  /**
   * Envia lo armado para la palabra en juego. Si no es la palabra, se consulta el diccionario:
   * no valida en void se rechaza (sin coste); en single, o si es valida, cuesta una vida.
   */
  suspend fun submit(letters: List<String>): Submission? {
    val s = solving() ?: return null
    val word = words[s.index]
    val verdict = when {
      letters == word.tiles -> Verdict.HIT
      isWord(letters) -> Verdict.NOT_PLAYED
      config.single -> Verdict.INVALID
      else -> return Submission.Rejected
    }
    if (solving() != s) return null
    resolve(s, verdict)
    return Submission.Resolved(verdict)
  }

  /** "No la recuerdo": se ve la palabra y cuesta una vida. */
  fun forget() {
    solving()?.let { resolve(it, Verdict.FORGOTTEN) }
  }

  /** Tras el veredicto: la siguiente palabra, el resumen de la partida o, sin vidas, el final. */
  fun next() {
    val s = stage as? Stage.Solving ?: return
    if (s.verdict == null) return
    stage = when {
      lives == 0 -> endSeries()
      s.index + 1 < words.size -> Stage.Solving(s.index + 1)
      else -> endRound()
    }
  }

  /** Tras [Stage.Failed]: se vuelve a pedir la partida. */
  fun retry() {
    if (stage is Stage.Failed) startRound()
  }

  fun nextRound() {
    if (stage != Stage.RoundDone) return
    round++
    startRound()
  }

  /** Otra serie con las mismas opciones. */
  fun restart() {
    lives = config.lives
    recalled = 0
    asked = 0
    round = 1
    best = records.best()
    record = null
    startRound()
  }

  /** La palabra en juego, si aun no tiene veredicto. */
  private fun solving(): Stage.Solving? = (stage as? Stage.Solving)?.takeIf { it.verdict == null }

  private fun resolve(s: Stage.Solving, verdict: Verdict) {
    val hit = verdict == Verdict.HIT
    answers = answers + Answer(words[s.index], hit)
    asked++
    if (hit) recalled++ else lives--
    stage = s.copy(verdict = verdict)
  }

  private fun endRound(): Stage = if (lives == 0) endSeries() else Stage.RoundDone

  private fun endSeries(): Stage {
    record = records.submit(recalled)
    return Stage.SeriesDone
  }

  private suspend fun isWord(letters: List<String>): Boolean =
    withContext(Dispatchers.Default) { source.isWord(dictionaryText(letters)) }

  /** Muestra la partida ya pedida y pide en segundo plano la siguiente. */
  private fun startRound() {
    stage = Stage.Loading
    answers = emptyList()
    val pending = next
    next = fetch()
    scope.launch { pending.await().fold(::watch) { stage = Stage.Failed(it.message ?: it.toString()) } }
  }

  private fun watch(round: RoundGame) {
    words = round.words
    stage = Stage.Watching(round.game)
  }

  // El fallo va en el Result: una excepcion suelta cancelaria el scope de la pantalla.
  private fun fetch(): Deferred<Result<RoundGame>> =
    scope.async(Dispatchers.Default) { runCatching { roundGame() } }

  /** La partida y sus palabras; elegir las fijas consulta el diccionario, asi que va aqui tambien. */
  private suspend fun roundGame(): RoundGame {
    val game = source.game()
    return RoundGame(game, Words.pick(game, config.wordsPerGame, random, source::isWord))
  }
}
