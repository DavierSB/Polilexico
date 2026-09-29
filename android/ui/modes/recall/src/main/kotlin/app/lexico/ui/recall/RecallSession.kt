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
import kotlin.random.Random

/** De donde salen las partidas a recordar (el motor, o datos de ejemplo). */
fun interface GameSource {
  /** Una partida entera, colocacion a colocacion. Puede tardar: se llama fuera del hilo principal. */
  suspend fun game(): List<ScoredPlacement>

  /**
   * Si las fichas juntas son una palabra valida (digrafos entre corchetes: "[CH]E"). Decide que
   * fichas fijas pueden ir pegadas; sin diccionario, ninguna. Tambien fuera del hilo principal.
   */
  fun isWord(word: String): Boolean = false
}

/** Una partida con sus palabras a recordar ya elegidas. */
private class RoundGame(val game: List<ScoredPlacement>, val words: List<WordToRecall>)

/** En que punto de la ronda esta la serie. */
sealed interface Stage {
  /** Esperando la partida (solo si el motor tarda mas que la ronda anterior). */
  data object Loading : Stage

  /** Se esta viendo la partida. */
  data class Watching(val game: List<ScoredPlacement>) : Stage

  /** Armando la palabra `index` de la ronda. */
  data class Solving(val index: Int) : Stage

  /** Ronda terminada: sus aciertos; si quedan rondas, se pasa a la siguiente. */
  data object RoundDone : Stage

  /** Serie terminada: el total, y la pregunta de si jugar otra. */
  data object SeriesDone : Stage

  /** No se pudo conseguir la partida; se puede reintentar. */
  data class Failed(val message: String) : Stage
}

/** Una palabra ya resuelta: `hit` = armada sin pedir verla. */
data class Answer(val word: WordToRecall, val hit: Boolean)

/**
 * Una serie de "¿Cuántas recuerdas?": [RecallConfig.rounds] rondas, cada una con una partida
 * nueva de la [source]. Mientras se juega una ronda ya se pide la partida de la siguiente, para
 * no esperar al motor entre rondas.
 */
@Stable
class RecallSession(
  val config: RecallConfig,
  private val source: GameSource,
  private val scope: CoroutineScope,
  private val random: Random = Random.Default,
) {
  var stage: Stage by mutableStateOf(Stage.Loading)
    private set

  /** La ronda en juego, desde 1. */
  var round: Int by mutableStateOf(1)
    private set

  /** Las palabras a recordar de esta ronda, en el orden en que se preguntan. */
  var words: List<WordToRecall> by mutableStateOf(emptyList())
    private set

  /** Lo resuelto en esta ronda. */
  var answers: List<Answer> by mutableStateOf(emptyList())
    private set

  /** Aciertos y palabras de cada ronda terminada de la serie. */
  var rounds: List<Pair<Int, Int>> by mutableStateOf(emptyList())
    private set

  val isLastRound: Boolean get() = round >= config.rounds

  private var next: Deferred<Result<RoundGame>> = fetch()

  init {
    startRound()
  }

  /** La partida se termino de ver: a armar sus palabras. */
  fun watched() {
    if (stage !is Stage.Watching) return
    stage = if (words.isEmpty()) endRound() else Stage.Solving(0)
  }

  /** La palabra en juego quedo resuelta: armada (`hit`) o vista. */
  fun answer(hit: Boolean) {
    val s = stage as? Stage.Solving ?: return
    answers = answers + Answer(words[s.index], hit)
    stage = if (s.index + 1 < words.size) Stage.Solving(s.index + 1) else endRound()
  }

  /** Tras [Stage.Failed]: se vuelve a pedir la partida de la ronda. */
  fun retry() {
    if (stage is Stage.Failed) startRound()
  }

  fun nextRound() {
    if (stage != Stage.RoundDone || isLastRound) return
    round++
    startRound()
  }

  /** Otra serie con las mismas opciones. */
  fun restart() {
    rounds = emptyList()
    round = 1
    startRound()
  }

  private fun endRound(): Stage {
    rounds = rounds + (answers.count { it.hit } to answers.size)
    return if (isLastRound) Stage.SeriesDone else Stage.RoundDone
  }

  /** Muestra la partida ya pedida y pide en segundo plano la de la ronda siguiente. */
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
    return RoundGame(game, Words.pick(game, config.wordsPerRound, random, source::isWord))
  }
}
