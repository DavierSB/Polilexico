package duplicate

import (
	"errors"
	"sync"
	"time"

	"lexico/engine/events"
	"lexico/engine/internal/notify"
	"lexico/engine/internal/timing"
)

// Las fases de la ronda, en Match.Phase.
const (
	PhaseWaiting     = "waiting"      // entre rondas: falta ver el atril (sin reloj)
	PhaseInvalidRack = "invalid_rack" // se muestra la mano invalida antes del atril bueno
	PhasePlaying     = "playing"      // pensando la jugada, con el reloj del turno
	PhaseConfirming  = "confirming"   // jugada propuesta: se anota sola si no se cancela a tiempo
	PhaseFinished    = "finished"
)

var (
	errPaused     = errors.New("la partida está en pausa")
	errNotWaiting = errors.New("la ronda ya empezó")
	errNotPlaying = errors.New("no es momento de jugar")
)

// Match es una duplicada en marcha: la partida (Game) y el ritmo de cada ronda, con sus plazos
// (el turno, la mano invalida, la ventana para cancelar), que el motor lleva solo. La
// plataforma pide ver el atril, propone jugadas, vuelve a leer el estado con cada aviso y pausa
// la partida cuando deja de verse. En pausa no corre ningun plazo; una cargada empieza en pausa.
type Match struct {
	mu       sync.Mutex
	game     *Game
	turn     *timing.Countdown
	step     *timing.Countdown
	turnTime time.Duration
	notifier *notify.Notifier
	phase    string
	draw     *Draw
	proposal *Attempt
	input    string
	lastErr  string
	paused   bool
	closed   bool
}

// NewMatch empieza una duplicada con turnMs por turno (0 = TurnSeconds). invalidLosesTurn:
// ver Game.SetInvalidPlayLosesTurn.
func NewMatch(turnMs int64, invalidLosesTurn bool, l events.Listener) (*Match, error) {
	g, err := Start()
	if err != nil {
		return nil, err
	}
	g.SetInvalidPlayLosesTurn(invalidLosesTurn)
	return startMatch(g, turnDuration(turnMs), timing.Real(), l, false), nil
}

// Game: la partida, para leer su estado (Status, Board, TurnAt, Unseen...).
func (m *Match) Game() *Game {
	return m.game
}

// ShowRack saca el atril de la ronda (en PhaseWaiting) y arranca su reloj.
func (m *Match) ShowRack() error {
	m.mu.Lock()
	defer m.mu.Unlock()
	if err := m.check(PhaseWaiting, errNotWaiting); err != nil {
		return err
	}
	draw, err := m.game.DrawRack()
	if err != nil {
		return err
	}
	m.enterDraw(draw)
	m.afterChange()
	return nil
}

// Propose comprueba tu jugada ("h8 CASA", "8h CASA" o "pasar"). Una colocacion queda por
// confirmar durante CancelSeconds; un pase o una jugada perdida se anotan al momento.
func (m *Match) Propose(input string) (*Attempt, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if err := m.check(PhasePlaying, errNotPlaying); err != nil {
		return nil, err
	}
	attempt, err := m.game.Validate(input)
	if err != nil {
		return nil, err
	}
	m.propose(attempt, input)
	m.afterChange()
	return attempt, nil
}

// Cancel deshace la jugada propuesta (en PhaseConfirming): se vuelve a pensar.
func (m *Match) Cancel() {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.phase != PhaseConfirming || m.paused {
		return
	}
	m.enterPlaying(false)
	m.afterChange()
}

// Pause para todos los plazos hasta Resume.
func (m *Match) Pause() {
	m.setPaused(true)
}

func (m *Match) Resume() {
	m.setPaused(false)
}

// Close detiene la partida: plazos y avisos. Guardala antes con Save.
func (m *Match) Close() {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.closed {
		return
	}
	m.closed = true
	m.afterChange()
	m.notifier.Close()
}

// startMatch pone en marcha (o en pausa) una partida ya creada, entre rondas.
func startMatch(g *Game, turnTime time.Duration, clock timing.Clock, l events.Listener, paused bool) *Match {
	m := &Match{game: g, turn: timing.NewCountdown(clock), step: timing.NewCountdown(clock), turnTime: turnTime,
		notifier: notify.New(l), phase: PhaseWaiting, paused: paused}
	if g.Status().Over {
		m.phase = PhaseFinished
	}
	return m
}

// check: nil si la partida esta en marcha y en la fase `want`; si no, el error que toca.
func (m *Match) check(want string, wrongPhase error) error {
	switch {
	case m.paused:
		return errPaused
	case m.phase != want:
		return wrongPhase
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

func turnDuration(turnMs int64) time.Duration {
	if turnMs <= 0 {
		return TurnSeconds * time.Second
	}
	return time.Duration(turnMs) * time.Millisecond
}
