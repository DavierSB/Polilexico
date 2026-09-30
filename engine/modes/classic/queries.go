package classic

import (
	pb "github.com/domino14/macondo/gen/api/proto/macondo"

	"lexico/engine/internal/core"
)

type Move struct {
	ByHuman    bool
	Kind       string
	Coords     string
	Tiles      string
	TileCount  int
	Score      int
	HumanTotal int
	BotTotal   int
}

type Status struct {
	Over          bool
	HumanToMove   bool
	HumanScore    int
	BotScore      int
	BagCount      int
	OpponentTiles int
}

type Result struct {
	Outcome    string
	LostOnTime bool
	EndReason  string
	HumanDelta int
	BotDelta   int
	HumanTime  int
	BotTime    int
	HumanScore int
	BotScore   int
	RecordPath string
}

func (c *Game) Status() *Status {
	c.mu.Lock()
	defer c.mu.Unlock()
	return &Status{Over: c.over(), HumanToMove: c.humanToMove(), HumanScore: c.score(true),
		BotScore: c.score(false), BagCount: c.g.Bag().TilesRemaining(),
		OpponentTiles: int(c.g.RackFor(c.botIdx()).NumTiles())}
}

func (c *Game) Rack() string {
	c.mu.Lock()
	defer c.mu.Unlock()
	return core.RackText(c.g, c.humanIdx)
}

func (c *Game) OpponentRack() string {
	c.mu.Lock()
	defer c.mu.Unlock()
	if !c.over() {
		return ""
	}
	return core.RackText(c.g, c.botIdx())
}

func (c *Game) Board() string {
	c.mu.Lock()
	defer c.mu.Unlock()
	return core.BoardText(c.g)
}

func (c *Game) Unseen() string {
	c.mu.Lock()
	defer c.mu.Unlock()
	return core.Unseen(c.g, c.g.RackFor(c.botIdx()).TilesOn()...)
}

func (c *Game) MoveCount() int {
	c.mu.Lock()
	defer c.mu.Unlock()
	return len(c.moves)
}

func (c *Game) MoveAt(i int) *Move {
	c.mu.Lock()
	defer c.mu.Unlock()
	if i < 0 || i >= len(c.moves) {
		return nil
	}
	return c.moves[i]
}

func (c *Game) Result() *Result {
	c.mu.Lock()
	defer c.mu.Unlock()
	if !c.over() {
		return nil
	}
	r := &Result{Outcome: c.outcome(), LostOnTime: c.lostOnTime, HumanScore: c.score(true),
		BotScore: c.score(false), HumanTime: c.timePenalty(true), BotTime: c.timePenalty(false), RecordPath: c.recordPath}
	if c.ending != nil {
		r.EndReason, r.HumanDelta, r.BotDelta = c.ending.Reason, c.ending.HumanDelta, c.ending.BotDelta
	}
	return r
}

func (c *Game) over() bool {
	return c.lostOnTime || c.ending != nil || c.g.Playing() == pb.PlayState_GAME_OVER
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
	return c.points(human) - c.timePenalty(human)
}

func (c *Game) points(human bool) int {
	if human {
		return c.g.PointsFor(c.humanIdx)
	}
	return c.g.PointsFor(c.botIdx())
}

func (c *Game) timePenalty(human bool) int {
	if human {
		return c.timePenalties[0]
	}
	return c.timePenalties[1]
}

func (c *Game) setTimePenalties(human, bot int) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.timePenalties = [2]int{human, bot}
}

func (c *Game) outcome() string {
	if c.lostOnTime {
		return core.OutcomeLoss
	}
	return core.Outcome(c.score(true), c.score(false))
}
