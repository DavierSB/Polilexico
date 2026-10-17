package duplicate

import (
	"errors"

	"lexico/engine/internal/core"
)

var (
	errNoTurn     = errors.New("primero hay que ver el atril")
	errNoExchange = errors.New("en duplicado no se cambian fichas: el atril se redibuja solo (regla FISF)")
)

type Attempt struct {
	Kind      string
	Coords    string
	Tiles     string
	Score     int
	Immediate bool
}

type Turn struct {
	Number       int
	Rack         string
	MasterCoords string
	MasterTiles  string
	MasterScore  int
	HumanKind    string
	HumanCoords  string
	HumanTiles   string
	HumanScore   int
	AssumedScore int
	Hit          bool
	AssumedHit   bool
	MasterTotal  int
	HumanTotal   int
	Hits         int
}

func (d *Game) Validate(input string) (*Attempt, error) {
	d.mu.Lock()
	defer d.mu.Unlock()
	play, assumed, err := d.humanPlay(input)
	if err != nil {
		return nil, err
	}
	return attemptOf(play, assumed), nil
}

func (d *Game) Confirm(input string) (*Turn, error) {
	d.mu.Lock()
	defer d.mu.Unlock()
	play, assumed, err := d.humanPlay(input)
	if err != nil {
		return nil, err
	}
	return d.closeTurn(play, assumed)
}

func (d *Game) TimeOut() (*Turn, error) {
	d.mu.Lock()
	defer d.mu.Unlock()
	if !d.inTurn() {
		return nil, errNoTurn
	}
	return d.closeTurn(core.Play{Kind: core.KindTimeout}, 0)
}

func (d *Game) humanPlay(input string) (core.Play, int, error) {
	if !d.inTurn() {
		return core.Play{}, 0, errNoTurn
	}
	if core.IsExchange(input) {
		return core.Play{}, 0, errNoExchange
	}
	return d.parse(input)
}

func (d *Game) parse(input string) (core.Play, int, error) {
	m, err := core.ParseInput(d.g, masterIdx, input)
	if invalid, ok := core.AsInvalidWords(err); ok && d.invalidLosesTurn {
		return invalid.Play, invalid.Score, nil
	}
	if err != nil {
		return core.Play{}, 0, err
	}
	play := core.PlayOf(m)
	return play, play.Score, nil
}

func (d *Game) closeTurn(human core.Play, assumed int) (*Turn, error) {
	master := d.plays[0]
	if err := d.g.PlayMove(master, true, 0); err != nil {
		return nil, err
	}
	d.g.SetPlayerOnTurn(masterIdx)
	turn := d.scoreTurn(human, assumed, core.PlayOf(master))
	d.turns = append(d.turns, turn)
	d.log.Turns = append(d.log.Turns, d.newTurnRecord(turn, human))
	d.plays = nil
	d.finishIfOver()
	return turn, nil
}

func (d *Game) scoreTurn(human core.Play, assumed int, master core.Play) *Turn {
	hit := human.Kind == core.KindPlay && human.Score == master.Score
	assumedHit := hit || human.Kind == core.KindInvalid && assumed == master.Score
	d.masterTotal += master.Score
	d.humanTotal += human.Score
	if hit {
		d.hits++
	}
	return &Turn{Number: d.turn, Rack: d.rack,
		MasterCoords: master.Coords, MasterTiles: master.Tiles, MasterScore: master.Score,
		HumanKind: human.Kind, HumanCoords: human.Coords, HumanTiles: human.Tiles, HumanScore: human.Score,
		AssumedScore: assumed, Hit: hit, AssumedHit: assumedHit, MasterTotal: d.masterTotal, HumanTotal: d.humanTotal, Hits: d.hits}
}

func attemptOf(p core.Play, assumed int) *Attempt {
	return &Attempt{Kind: p.Kind, Coords: p.Coords, Tiles: p.Tiles, Score: assumed,
		Immediate: p.Kind != core.KindPlay && p.Kind != core.KindInvalid}
}
