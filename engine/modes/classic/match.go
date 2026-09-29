package classic

import (
	"errors"
	"sync"

	"lexico/engine/events"
	"lexico/engine/internal/notify"
	"lexico/engine/internal/timing"
)

var errPaused = errors.New("la partida está en pausa")

// Match es una partida clasica en marcha: la partida (Game), sus relojes y el bot, que juega
// solo cuando le toca. Todo lo decide el motor; la plataforma envia tus jugadas, vuelve a leer
// el estado cuando recibe un aviso y pausa la partida cuando deja de verse.
//
// Se crea en marcha; una cargada con LoadMatch empieza en pausa. En pausa no corre ningun reloj
// y el bot no juega: si le tocaba, juega al continuar.
type Match struct {
	mu       sync.Mutex
	game     *Game
	clocks   *clockPair // nil = partida sin tiempo
	timeout  *timing.Alarm
	notifier *notify.Notifier
	paused   bool
	closed   bool
	thinking bool
	botError string
	// hideUnseen: la bolsa no muestra las fichas por salir, solo cuantas quedan.
	hideUnseen bool
}

// NewMatch empieza una partida contra botName (uno de Bots()). Con timeMs 0 no hay relojes;
// si no, cada jugador tiene timeMs y, al agotarlos, overtimeMs de descuento: quien agota tambien
// el descuento pierde por tiempo. invalidLosesTurn: ver Game.SetInvalidPlayLosesTurn.
func NewMatch(botName string, timeMs, overtimeMs int64, invalidLosesTurn bool, l events.Listener) (*Match, error) {
	g, err := Start(botName)
	if err != nil {
		return nil, err
	}
	return newMatch(g, timeMs, overtimeMs, invalidLosesTurn, l), nil
}

// Game: la partida, para leer su estado (Status, Board, Rack, MoveAt...).
func (m *Match) Game() *Game {
	return m.game
}

// Play hace tu jugada, como Game.Play; despues, si le toca, el bot juega solo.
func (m *Match) Play(input string) (*Move, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.paused {
		return nil, errPaused
	}
	played, err := m.game.Play(input)
	if err == nil {
		m.afterChange()
	}
	return played, err
}

// Pause para los relojes y el bot hasta Resume.
func (m *Match) Pause() {
	m.setPaused(true)
}

func (m *Match) Resume() {
	m.setPaused(false)
}

func (m *Match) Paused() bool {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.paused
}

// BotError: por que fallo el ultimo turno del bot ("" si no fallo).
func (m *Match) BotError() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.botError
}

// SetShowUnseen elige si la bolsa muestra las fichas por salir (la bolsa y el atril del rival)
// o solo cuantas quedan. Se guarda con la partida.
func (m *Match) SetShowUnseen(show bool) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.hideUnseen = !show
}

func (m *Match) ShowUnseen() bool {
	m.mu.Lock()
	defer m.mu.Unlock()
	return !m.hideUnseen
}

// Close detiene la partida: relojes, temporizadores y avisos. Guardala antes con Save.
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

// newMatch pone en marcha la partida g con relojes reales (ver NewMatch).
func newMatch(g *Game, timeMs, overtimeMs int64, invalidLosesTurn bool, l events.Listener) *Match {
	g.SetInvalidPlayLosesTurn(invalidLosesTurn)
	clock := timing.Real()
	return startMatch(g, newClockPair(clock, timeMs, overtimeMs, 0, 0), clock, l, false)
}

// startMatch pone en marcha (o en pausa) una partida ya creada.
func startMatch(g *Game, clocks *clockPair, clock timing.Clock, l events.Listener, paused bool) *Match {
	m := &Match{game: g, clocks: clocks, timeout: timing.NewAlarm(clock), notifier: notify.New(l), paused: paused}
	m.mu.Lock()
	defer m.mu.Unlock()
	m.afterChange()
	return m
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
