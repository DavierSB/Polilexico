package classic

import (
	"errors"
	"strings"

	"github.com/domino14/macondo/game"
	pb "github.com/domino14/macondo/gen/api/proto/macondo"

	"lexico/engine/internal/core"
)

var (
	errNothingToClaim = errors.New("no hay jugada que reclamar")
	errFinalPass      = errors.New("solo puedes reclamar o pasar")
)

type Claim struct {
	Seq     int
	Upheld  bool
	ByHuman bool
	Words   string
	Invalid string
	Score   int
}

func (m *Match) SetClaims(on bool) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.closed {
		return
	}
	m.game.setClaims(on)
	m.afterChange()
}

func (c *Game) Claims() bool {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.claims
}

func (c *Game) Claimable() bool {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.claimable()
}

func (c *Game) FinalPass() bool {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.finalPass && !c.over()
}

func (c *Game) LastClaim() *Claim {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.lastClaim
}

func (c *Game) setClaims(on bool) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if on {
		c.enableClaims()
	} else {
		c.disableClaims()
	}
}

func (c *Game) enableClaims() {
	if c.claims || len(c.moves) > 0 {
		return
	}
	c.claims = true
	c.g.SetChallengeRule(pb.ChallengeRule_SINGLE)
	c.g.SetBackupMode(game.InteractiveGameplayMode)
	c.g.SetStateStackLength(1)
}

func (c *Game) disableClaims() {
	if !c.claims {
		return
	}
	if c.claimable() && (c.finalPass || c.lastIsPhony()) {
		c.claim()
	}
	c.claims = false
}

func (c *Game) resumeClaims() {
	if c.claims {
		c.g.SetBackupMode(game.InteractiveGameplayMode)
	}
}

func (c *Game) claimable() bool {
	n := len(c.moves)
	return c.claims && !c.over() && n > 0 && c.moves[n-1].Kind == core.KindPlay && c.claimedAt != n
}

func (c *Game) lastIsPhony() bool {
	_, invalid := core.LastWords(c.g)
	return len(invalid) > 0
}

func (c *Game) claim() (*Move, error) {
	if !c.claimable() {
		return nil, errNothingToClaim
	}
	words, invalid := core.LastWords(c.g)
	last, byHuman := c.moves[len(c.moves)-1], c.humanToMove()
	legal, err := c.g.ChallengeEvent(0, 0)
	if err != nil {
		return nil, err
	}
	c.noteClaim(!legal, byHuman, last.Score, words, invalid)
	if !legal {
		c.withdrawLast(words)
	}
	c.claimedAt = len(c.moves)
	c.afterTurn()
	return c.moves[len(c.moves)-1], nil
}

func (c *Game) noteClaim(upheld, byHuman bool, score int, words, invalid []string) {
	seq := 1
	if c.lastClaim != nil {
		seq = c.lastClaim.Seq + 1
	}
	c.lastClaim = &Claim{Seq: seq, Upheld: upheld, ByHuman: byHuman, Score: score,
		Words: strings.Join(words, " "), Invalid: strings.Join(invalid, " ")}
}

func (c *Game) withdrawLast(words []string) {
	last := c.moves[len(c.moves)-1]
	p := core.Play{Kind: core.KindInvalid, Coords: last.Coords, Tiles: last.Tiles, TileCount: last.TileCount, Words: words}
	c.moves[len(c.moves)-1] = moveOf(p, last.ByHuman, c.points(true), c.points(false))
	c.withdrawRecord(p)
}

func (c *Game) withdrawRecord(p core.Play) {
	if n := len(c.log.Turns); n > 0 {
		r := &c.log.Turns[n-1]
		r.ActualPlay = core.CandidateOf(p, r.ActualPlay.Equity)
		r.WasBest = false
	}
}

func (c *Game) settleFinalPass() error {
	if !c.claims && c.waitingFinalPass() {
		if _, err := c.g.ChallengeEvent(0, 0); err != nil {
			return err
		}
	}
	c.finalPass = c.waitingFinalPass()
	return nil
}

func (c *Game) waitingFinalPass() bool {
	return c.g.Playing() == pb.PlayState_WAITING_FOR_FINAL_PASS
}

func (c *Game) afterTurn() {
	c.finalPass = c.waitingFinalPass()
	c.dropOutBonus()
	c.settleEnding()
	c.finishIfOver()
}

func (c *Game) checkFinalPass(input string) error {
	if c.finalPass && !core.IsPass(input) {
		return errFinalPass
	}
	return nil
}
