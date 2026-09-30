package classic

import (
	pb "github.com/domino14/macondo/gen/api/proto/macondo"

	"lexico/engine/internal/core"
)

const (
	EndOut     = "out"
	EndPasses  = "passes"
	EndNeutral = "neutral"
)

const (
	passesToEnd    = 4
	neutralToEnd   = 12
	noScorelessEnd = 1 << 30
)

type Ending struct {
	Reason     string
	HumanDelta int
	BotDelta   int
}

func (c *Game) dropOutBonus() {
	if goer, ok := c.wentOut(); ok {
		bonus := 2 * core.RackValue(c.g, 1-goer)
		c.g.SetPointsFor(goer, c.g.PointsFor(goer)-bonus)
	}
}

func (c *Game) settleEnding() {
	reason := c.endReason()
	if reason == "" {
		return
	}
	c.ending = c.endingFor(reason)
	c.applyEnding()
}

func (c *Game) applyEnding() {
	if c.ending == nil {
		return
	}
	last := c.moves[len(c.moves)-1]
	c.g.SetPointsFor(c.humanIdx, last.HumanTotal+c.ending.HumanDelta)
	c.g.SetPointsFor(c.botIdx(), last.BotTotal+c.ending.BotDelta)
	c.g.SetPlaying(pb.PlayState_GAME_OVER)
}

func (c *Game) endReason() string {
	switch {
	case c.g.Playing() == pb.PlayState_GAME_OVER:
		return EndOut
	case c.trailing(passesToEnd, isPass):
		return EndPasses
	case c.trailing(neutralToEnd, isNeutral):
		return EndNeutral
	}
	return ""
}

func (c *Game) endingFor(reason string) *Ending {
	human, bot := core.RackValue(c.g, c.humanIdx), core.RackValue(c.g, c.botIdx())
	if reason != EndOut {
		return &Ending{Reason: reason, HumanDelta: -human, BotDelta: -bot}
	}
	return &Ending{Reason: reason, HumanDelta: bot - human, BotDelta: human - bot}
}

func (c *Game) wentOut() (int, bool) {
	if c.g.Playing() != pb.PlayState_GAME_OVER {
		return 0, false
	}
	for i := 0; i < 2; i++ {
		if c.g.RackFor(i).NumTiles() == 0 {
			return i, true
		}
	}
	return 0, false
}

func (c *Game) trailing(n int, counts func(*Move) bool) bool {
	if len(c.moves) < n {
		return false
	}
	for _, m := range c.moves[len(c.moves)-n:] {
		if !counts(m) {
			return false
		}
	}
	return true
}

func isPass(m *Move) bool {
	return m.Kind == core.KindPass || m.Kind == core.KindInvalid
}

func isNeutral(m *Move) bool {
	return isPass(m) || m.Kind == core.KindExchange
}
