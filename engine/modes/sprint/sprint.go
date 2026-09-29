// Package sprint es Scrabble Sprint: HastyBot juega partidas contra si mismo y, de vez en
// cuando, se para en un turno en que el jugador en turno podia colocar un scrabble (las 7
// fichas del atril) y te lo propone: el tablero y ese atril. Es un arcade: hay un reloj para
// toda la serie, que solo corre mientras piensas una mano, y unas vidas (de 1 a MaxLives);
// rendirte en una mano cuesta una. La serie termina al agotarse el reloj o las vidas, y cuenta
// las manos resueltas.
//
// Cuando se para: se cuentan los turnos con scrabble posible y se para al llegar a un numero
// elegido al azar entre 1 y 20 (uniforme); entonces se vuelve a contar desde cero con un
// numero nuevo. Tras cada problema la partida sigue donde estaba; al terminar, empieza otra.
//
// Match es la serie en marcha: la busqueda, el reloj de la serie (con su pausa) y las vidas.
// La plataforma envia las respuestas, vuelve a leer el estado con cada aviso y pausa la serie
// cuando deja de verse. Todos sus metodos se pueden llamar desde cualquier hilo.
package sprint

import (
	"errors"
	"math/rand/v2"
	"sync"
	"time"

	"lexico/engine/events"
	"lexico/engine/internal/core"
	"lexico/engine/internal/notify"
	"lexico/engine/internal/timing"
)

// Reglas de la serie.
const (
	Lives        = 3   // vidas al empezar, por defecto
	MaxLives     = 5   // vidas al empezar, como mucho
	MatchSeconds = 300 // tiempo de la serie por defecto
	MaxTarget    = 20  // el numero de turnos con scrabble se elige entre 1 y MaxTarget
)

// Las fases de la serie, en Match.Phase.
const (
	PhaseSearching = "searching" // HastyBot busca el siguiente problema (sin reloj)
	PhaseSolving   = "solving"   // pensando el scrabble, con el reloj de la serie en marcha
	PhaseRevealed  = "revealed"  // mano cerrada: se ven sus scrabbles hasta pedir la siguiente
	PhaseFinished  = "finished"  // sin tiempo o sin vidas (o la busqueda fallo: ver LastError)
)

// Como se cerro la ultima mano, en Match.Outcome.
const (
	OutcomeSolved  = "solved"  // colocaste un scrabble
	OutcomeTimeout = "timeout" // se agoto el reloj de la serie: se acabo
	OutcomeGaveUp  = "gave_up" // te rendiste
)

var (
	errPaused     = errors.New("la partida está en pausa")
	errNotSolving = errors.New("no hay ninguna mano en juego")
)

// Match es una serie de Scrabble Sprint en marcha. En pausa no corre el reloj.
type Match struct {
	mu        sync.Mutex
	hunter    *hunter
	clock     *timing.Countdown
	totalTime time.Duration
	maxLives  int
	notifier  *notify.Notifier
	phase    string
	puzzle   *puzzle
	// Marcador: vidas que quedan, manos resueltas y manos propuestas.
	lives, solved, posed int
	outcome              string
	answer               *Solution // tu scrabble, si resolviste la ultima mano
	lastErr              string
	paused, closed       bool
}

// NewMatch empieza una serie con totalMs en el reloj (0 = MatchSeconds) y `lives` vidas (fuera
// de 1..MaxLives, Lives). Empieza buscando el primer problema; l recibe un aviso con cada cambio.
func NewMatch(totalMs int64, lives int, l events.Listener) (*Match, error) {
	if err := core.Ready(); err != nil {
		return nil, err
	}
	return startMatch(totalDuration(totalMs), startingLives(lives), timing.Real(), randomTarget, l), nil
}

// Propose comprueba tu respuesta ("h8 CASADOS" u "8h CASADOS"): tiene que colocar las 7
// fichas del atril y formar palabras validas. Si lo es, la mano queda resuelta; si no, se
// rechaza con el motivo y puedes seguir intentandolo mientras quede reloj.
func (m *Match) Propose(input string) (*Solution, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if err := m.checkSolving(); err != nil {
		return nil, err
	}
	s, err := m.puzzle.check(input)
	if err != nil {
		return nil, err
	}
	m.reveal(OutcomeSolved, &s)
	m.afterChange()
	return &s, nil
}

// GiveUp cierra la mano sin resolverla: cuesta una vida.
func (m *Match) GiveUp() error {
	m.mu.Lock()
	defer m.mu.Unlock()
	if err := m.checkSolving(); err != nil {
		return err
	}
	m.reveal(OutcomeGaveUp, nil)
	m.afterChange()
	return nil
}

// Next pasa de una mano cerrada (PhaseRevealed) a buscar la siguiente.
func (m *Match) Next() {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.phase != PhaseRevealed || m.paused {
		return
	}
	m.search()
	m.afterChange()
}

// Pause para el reloj hasta Resume.
func (m *Match) Pause() {
	m.setPaused(true)
}

func (m *Match) Resume() {
	m.setPaused(false)
}

// Close detiene la serie: la busqueda, el reloj y los avisos.
func (m *Match) Close() {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.closed {
		return
	}
	m.closed = true
	m.hunter.stop()
	m.afterChange()
	m.notifier.Close()
}

// startMatch pone en marcha una serie nueva: con el reloj entero y todas las vidas, buscando el
// primer problema. pick elige cada cuantos turnos con scrabble posible se para la busqueda.
func startMatch(total time.Duration, lives int, clock timing.Clock, pick func() int, l events.Listener) *Match {
	m := &Match{hunter: startHunter(pick), clock: timing.NewCountdown(clock), totalTime: total,
		maxLives: lives, notifier: notify.New(l), lives: lives}
	m.mu.Lock()
	defer m.mu.Unlock()
	m.clock.Restart(total)
	m.search()
	m.afterChange()
	return m
}

// checkSolving: nil si la serie esta en marcha y con una mano en juego.
func (m *Match) checkSolving() error {
	switch {
	case m.paused:
		return errPaused
	case m.phase != PhaseSolving:
		return errNotSolving
	}
	return nil
}

func (m *Match) setPaused(paused bool) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.closed || m.paused == paused {
		return
	}
	m.paused = paused
	m.afterChange()
}

// randomTarget: un numero entre 1 y MaxTarget, uniforme.
func randomTarget() int {
	return rand.IntN(MaxTarget) + 1
}

func totalDuration(totalMs int64) time.Duration {
	if totalMs <= 0 {
		return MatchSeconds * time.Second
	}
	return time.Duration(totalMs) * time.Millisecond
}

func startingLives(lives int) int {
	if lives < 1 || lives > MaxLives {
		return Lives
	}
	return lives
}
