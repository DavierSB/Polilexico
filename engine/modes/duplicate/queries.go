package duplicate

import (
	pb "github.com/domino14/macondo/gen/api/proto/macondo"

	"lexico/engine/internal/core"
)

type Status struct {
	Over        bool
	InTurn      bool
	Turn        int
	TurnsPlayed int
	HumanTotal  int
	MasterTotal int
	Hits        int
	BagCount    int
}

type Result struct {
	Outcome     string
	HumanTotal  int
	MasterTotal int
	Hits        int
	Turns       int
	RecordPath  string
}

func (d *Game) Status() *Status {
	d.mu.Lock()
	defer d.mu.Unlock()
	return &Status{Over: d.over(), InTurn: d.inTurn(), Turn: d.currentTurn(), TurnsPlayed: len(d.turns),
		HumanTotal: d.humanTotal, MasterTotal: d.masterTotal, Hits: d.hits,
		BagCount: d.g.Bag().TilesRemaining()}
}

func (d *Game) Board() string {
	d.mu.Lock()
	defer d.mu.Unlock()
	return core.BoardText(d.g)
}

func (d *Game) Unseen() string {
	d.mu.Lock()
	defer d.mu.Unlock()
	return core.Unseen(d.g)
}

func (d *Game) TurnCount() int {
	d.mu.Lock()
	defer d.mu.Unlock()
	return len(d.turns)
}

func (d *Game) TurnAt(i int) *Turn {
	d.mu.Lock()
	defer d.mu.Unlock()
	if i < 0 || i >= len(d.turns) {
		return nil
	}
	return d.turns[i]
}

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
