package duplicate

import (
	"errors"

	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
)

const (
	EndNoValidRack    = "no_valid_rack"
	EndNoPlayableRack = "no_playable_rack"
)

const (
	maxDrawAttempts      = 10000
	maxUnplayableRacks   = 40
	maxShownInvalidRacks = 5
	reducedFromTurn      = 16
	maxPerKind           = 5
)

var errGameOver = errors.New("la partida ya terminó")

type Draw struct {
	Rack         string
	Redrawn      bool
	InvalidCount int
	GameOver     bool
	EndReason    string
	invalidRacks []string
	unplayable   int
}

func (d *Game) DrawRack() (*Draw, error) {
	d.mu.Lock()
	defer d.mu.Unlock()
	if d.over() {
		return nil, errGameOver
	}
	if d.inTurn() {
		return &Draw{Rack: d.rack}, nil
	}
	d.turn++
	return d.draw(), nil
}

func (draw *Draw) ManyInvalid() bool {
	return draw.InvalidCount > maxShownInvalidRacks
}

func (d *Game) draw() *Draw {
	draw := &Draw{}
	reduced := d.reducedMinimum()
	for i := 0; i < maxDrawAttempts && draw.unplayable < maxUnplayableRacks; i++ {
		if done := d.tryRack(draw, reduced); done != nil {
			return done
		}
		d.noteInvalid(draw)
		if !d.redraw() {
			break
		}
	}
	return d.endGame(draw, EndNoPlayableRack)
}

func (d *Game) tryRack(draw *Draw, reduced bool) *Draw {
	switch {
	case !d.poolIsValid():
		return d.endGame(draw, EndNoValidRack)
	case !d.rackIsValid(reduced):
		return nil
	case d.startTurn():
		draw.Rack = d.rack
		return draw
	}
	draw.unplayable++
	return nil
}

func (d *Game) startTurn() bool {
	plays := d.masterPlays()
	if len(plays) == 0 || plays[0].Action() == move.MoveTypePass {
		return false
	}
	d.plays = plays
	d.rack = core.RackText(d.g, masterIdx)
	d.boardBefore = core.RenderBoard(d.g)
	return true
}

func (d *Game) noteInvalid(draw *Draw) {
	draw.Redrawn = true
	draw.InvalidCount++
	if len(draw.invalidRacks) < maxShownInvalidRacks {
		draw.invalidRacks = append(draw.invalidRacks, core.RackText(d.g, masterIdx))
	}
}

func (d *Game) redraw() bool {
	if d.g.Bag().TilesRemaining() == 0 {
		return false
	}
	_, err := d.g.SetRandomRack(masterIdx, nil)
	return err == nil
}

func (d *Game) endGame(draw *Draw, reason string) *Draw {
	d.ended = true
	d.finish()
	draw.GameOver, draw.EndReason = true, reason
	return draw
}

func (d *Game) reducedMinimum() bool {
	vowels, consonants := d.countPure(d.pool())
	return d.turn >= reducedFromTurn || vowels <= 1 || consonants <= 1
}

func (d *Game) rackIsValid(reduced bool) bool {
	vowels, consonants := d.countPure(d.g.RackFor(masterIdx).TilesOn())
	if reduced {
		return vowels >= 1 && consonants >= 1
	}
	return vowels <= maxPerKind && consonants <= maxPerKind
}

func (d *Game) poolIsValid() bool {
	vowels, consonants := d.countPure(d.pool())
	return vowels >= 1 && consonants >= 1
}

func (d *Game) pool() []tilemapping.MachineLetter {
	return append(append([]tilemapping.MachineLetter{}, d.g.Bag().Peek()...), d.g.RackFor(masterIdx).TilesOn()...)
}

func (d *Game) countPure(tiles []tilemapping.MachineLetter) (vowels, consonants int) {
	ld := d.g.Bag().LetterDistribution()
	for _, t := range tiles {
		switch {
		case t == 0:
		case t.IsVowel(ld):
			vowels++
		default:
			consonants++
		}
	}
	return vowels, consonants
}
