package duplicate

import (
	"errors"
	"sort"

	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
)

const (
	EndNoValidRack    = "no_valid_rack"
	EndNoPlayableRack = "no_playable_rack"
)

const maxDrawAttempts = 40

var errGameOver = errors.New("la partida ya terminó")

type Draw struct {
	Rack        string
	Redrawn     bool
	InitialRack string
	GameOver    bool
	EndReason   string
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

func (d *Game) draw() *Draw {
	draw := &Draw{}
	for i := 0; i < maxDrawAttempts; i++ {
		if done := d.tryRack(draw); done != nil {
			return done
		}
		if !d.redraw() {
			break
		}
	}
	return d.endGame(draw, EndNoPlayableRack)
}

func (d *Game) tryRack(draw *Draw) *Draw {
	switch {
	case !d.rackIsValid() && !d.poolIsValid():
		return d.endGame(draw, EndNoValidRack)
	case !d.rackIsValid():
		d.noteRedraw(draw)
	case d.startTurn():
		draw.Rack = d.rack
		return draw
	}
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

func (d *Game) masterPlays() []*move.Move {
	plays := d.master.GenAll(d.g.RackFor(masterIdx), false)
	sort.Slice(plays, func(i, j int) bool { return plays[i].TiebreaksBetter(plays[j]) })
	return copyMoves(plays)
}

func (d *Game) noteRedraw(draw *Draw) {
	if !draw.Redrawn {
		draw.Redrawn = true
		draw.InitialRack = core.RackText(d.g, masterIdx)
	}
}

func (d *Game) redraw() bool {
	_, err := d.g.SetRandomRack(masterIdx, nil)
	return err == nil
}

func (d *Game) endGame(draw *Draw, reason string) *Draw {
	d.ended = true
	d.finish()
	draw.GameOver, draw.EndReason = true, reason
	return draw
}

func (d *Game) rackIsValid() bool {
	return d.validTiles(d.g.RackFor(masterIdx).TilesOn())
}

func (d *Game) poolIsValid() bool {
	pool := append(append([]tilemapping.MachineLetter{}, d.g.Bag().Peek()...), d.g.RackFor(masterIdx).TilesOn()...)
	return d.validTiles(pool)
}

func (d *Game) validTiles(tiles []tilemapping.MachineLetter) bool {
	minVowels, minConsonants := minimums(d.turn)
	vowels, consonants, blanks := countTiles(tiles, d.g.Bag().LetterDistribution())
	return vowels+blanks >= minVowels && consonants+blanks >= minConsonants
}

func minimums(turn int) (vowels, consonants int) {
	if turn > 15 {
		return 1, 1
	}
	return 2, 2
}

func countTiles(tiles []tilemapping.MachineLetter, ld *tilemapping.LetterDistribution) (vowels, consonants, blanks int) {
	for _, t := range tiles {
		switch {
		case t == 0:
			blanks++
		case t.IsVowel(ld):
			vowels++
		default:
			consonants++
		}
	}
	return vowels, consonants, blanks
}

func copyMoves(moves []*move.Move) []*move.Move {
	out := make([]*move.Move, len(moves))
	for i, m := range moves {
		out[i] = new(move.Move)
		out[i].CopyFrom(m)
	}
	return out
}
