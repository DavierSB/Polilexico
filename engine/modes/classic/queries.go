package classic

import (
	pb "github.com/domino14/macondo/gen/api/proto/macondo"

	"lexico/engine/internal/core"
)

// Move es una jugada ya hecha, para que la interfaz la dibuje.
type Move struct {
	ByHuman   bool
	Kind      string // "play", "pass", "exchange" o "invalid"
	Coords    string // "H8" / "8H" en play e invalid
	Tiles     string // "CA.A" ('.' = letra ya en el tablero); en exchange, las fichas ("" si cambio el bot)
	TileCount int    // fichas colocadas o cambiadas
	Score     int
	// Marcador tras la jugada.
	HumanTotal int
	BotTotal   int
}

// Status es el estado de la partida en este momento.
type Status struct {
	Over          bool
	HumanToMove   bool
	HumanScore    int
	BotScore      int
	BagCount      int
	OpponentTiles int // fichas del bot (se dibujan boca abajo)
}

// Result es el resultado de una partida terminada.
type Result struct {
	Outcome    string // "win", "loss" o "tie"
	LostOnTime bool
	HumanScore int
	BotScore   int
	RecordPath string // el -log.json de la partida ("" si no se pudo escribir)
}

// Status devuelve el estado actual.
func (c *Game) Status() *Status {
	c.mu.Lock()
	defer c.mu.Unlock()
	return &Status{Over: c.over(), HumanToMove: c.humanToMove(), HumanScore: c.score(true),
		BotScore: c.score(false), BagCount: c.g.Bag().TilesRemaining(),
		OpponentTiles: int(c.g.RackFor(c.botIdx()).NumTiles())}
}

// Rack: tus fichas ("A CH E ?"), en el orden del motor.
func (c *Game) Rack() string {
	c.mu.Lock()
	defer c.mu.Unlock()
	return core.RackText(c.g, c.humanIdx)
}

// OpponentRack: las fichas del bot; solo se revelan al terminar (antes, "").
func (c *Game) OpponentRack() string {
	c.mu.Lock()
	defer c.mu.Unlock()
	if !c.over() {
		return ""
	}
	return core.RackText(c.g, c.botIdx())
}

// Board: el tablero, en el formato de core.BoardText ("." vacia, minuscula = comodin).
func (c *Game) Board() string {
	c.mu.Lock()
	defer c.mu.Unlock()
	return core.BoardText(c.g)
}

// Unseen: las fichas que no ves (bolsa y atril del bot), ordenadas.
func (c *Game) Unseen() string {
	c.mu.Lock()
	defer c.mu.Unlock()
	return core.Unseen(c.g, c.g.RackFor(c.botIdx()).TilesOn()...)
}

// MoveCount: cuantas jugadas lleva la partida.
func (c *Game) MoveCount() int {
	c.mu.Lock()
	defer c.mu.Unlock()
	return len(c.moves)
}

// MoveAt devuelve la jugada i (0 = la primera), o nil si no existe.
func (c *Game) MoveAt(i int) *Move {
	c.mu.Lock()
	defer c.mu.Unlock()
	if i < 0 || i >= len(c.moves) {
		return nil
	}
	return c.moves[i]
}

// Result devuelve el resultado, o nil si la partida sigue.
func (c *Game) Result() *Result {
	c.mu.Lock()
	defer c.mu.Unlock()
	if !c.over() {
		return nil
	}
	return &Result{Outcome: c.outcome(), LostOnTime: c.lostOnTime, HumanScore: c.score(true),
		BotScore: c.score(false), RecordPath: c.recordPath}
}

func (c *Game) over() bool {
	return c.lostOnTime || c.g.Playing() == pb.PlayState_GAME_OVER
}

func (c *Game) humanToMove() bool {
	return !c.over() && c.g.PlayerOnTurn() == c.humanIdx
}

func (c *Game) botToMove() bool {
	return !c.over() && c.g.PlayerOnTurn() == c.botIdx()
}

func (c *Game) botIdx() int {
	return 1 - c.humanIdx
}

func (c *Game) score(human bool) int {
	if human {
		return c.g.PointsFor(c.humanIdx)
	}
	return c.g.PointsFor(c.botIdx())
}

func (c *Game) outcome() string {
	if c.lostOnTime {
		return core.OutcomeLoss
	}
	return core.Outcome(c.score(true), c.score(false))
}
