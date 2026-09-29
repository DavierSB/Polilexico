package duplicate

import (
	pb "github.com/domino14/macondo/gen/api/proto/macondo"

	"lexico/engine/internal/core"
)

// Status es el estado de la partida en este momento.
type Status struct {
	Over        bool
	InTurn      bool // ya se vio el atril y falta confirmar la jugada
	Turn        int  // el turno en curso o, entre turnos, el siguiente
	TurnsPlayed int
	HumanTotal  int
	MasterTotal int
	Hits        int
	BagCount    int
}

// Result es el resultado de una partida terminada.
type Result struct {
	Outcome     string // "win", "loss" o "tie" frente al máster
	HumanTotal  int
	MasterTotal int
	Hits        int
	Turns       int
	RecordPath  string // el -log.json de la partida ("" si no se pudo escribir)
}

// Status devuelve el estado actual.
func (d *Game) Status() *Status {
	d.mu.Lock()
	defer d.mu.Unlock()
	return &Status{Over: d.over(), InTurn: d.inTurn(), Turn: d.currentTurn(), TurnsPlayed: len(d.turns),
		HumanTotal: d.humanTotal, MasterTotal: d.masterTotal, Hits: d.hits,
		BagCount: d.g.Bag().TilesRemaining()}
}

// Board: el tablero, en el formato de core.BoardText ("." vacia, minuscula = comodin).
func (d *Game) Board() string {
	d.mu.Lock()
	defer d.mu.Unlock()
	return core.BoardText(d.g)
}

// Unseen: las fichas que quedan en la bolsa, ordenadas (el atril ya se ve).
func (d *Game) Unseen() string {
	d.mu.Lock()
	defer d.mu.Unlock()
	return core.Unseen(d.g)
}

// TurnCount: cuantos turnos se han jugado.
func (d *Game) TurnCount() int {
	d.mu.Lock()
	defer d.mu.Unlock()
	return len(d.turns)
}

// TurnAt devuelve el turno i (0 = el primero), o nil si no existe.
func (d *Game) TurnAt(i int) *Turn {
	d.mu.Lock()
	defer d.mu.Unlock()
	if i < 0 || i >= len(d.turns) {
		return nil
	}
	return d.turns[i]
}

// Result devuelve el resultado, o nil si la partida sigue.
func (d *Game) Result() *Result {
	d.mu.Lock()
	defer d.mu.Unlock()
	if !d.over() {
		return nil
	}
	return &Result{Outcome: core.Outcome(d.humanTotal, d.masterTotal), HumanTotal: d.humanTotal,
		MasterTotal: d.masterTotal, Hits: d.hits, Turns: len(d.turns), RecordPath: d.recordPath}
}

func (d *Game) over() bool {
	return d.ended || d.g.Playing() == pb.PlayState_GAME_OVER
}

func (d *Game) inTurn() bool {
	return d.plays != nil
}

func (d *Game) currentTurn() int {
	if d.inTurn() {
		return d.turn
	}
	return d.turn + 1
}
