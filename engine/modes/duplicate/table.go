package duplicate

import (
	"errors"
	"strings"
	"sync"
	"time"

	"lexico/engine/events"
	"lexico/engine/internal/core"
	"lexico/engine/internal/notify"
	"lexico/engine/internal/timing"
)

const (
	TablePlaying  = "playing"
	TableResults  = "results"
	TableFinished = "finished"
)

const masterName = "Máster"

var (
	errNoPlayer   = errors.New("ese jugador no está en la mesa")
	errRoundEnded = errors.New("la ronda ya terminó")
)

type Table struct {
	mu       sync.Mutex
	game     *Game
	players  []string
	inputs   map[string]string
	attempts map[string]*Attempt
	ready    map[string]bool
	absent   map[string]bool
	rounds   []*tableRound
	totals   map[string]int
	phase    string
	turn     *timing.Countdown
	turnTime time.Duration
	live     bool
	notifier *notify.Notifier
	closed   bool
}

type tableRound struct {
	Number int
	Rack   string
	Plays  []*TablePlay
}

type TablePlay struct {
	Name   string
	Kind   string
	Coords string
	Tiles  string
	Points int
	Total  int
}

func NewTable(players string, turnMs int64, maxRounds int, live bool, l events.Listener) (*Table, error) {
	g, err := Start()
	if err != nil {
		return nil, err
	}
	g.SetInvalidPlayLosesTurn(true)
	g.SetMaxRounds(maxRounds)
	return startTable(g, strings.Split(players, "\n"), turnDuration(turnMs), timing.Real(), live, l), nil
}

func (t *Table) Propose(player, input string) (*Attempt, error) {
	t.mu.Lock()
	defer t.mu.Unlock()
	if err := t.checkPlaying(player); err != nil {
		return nil, err
	}
	attempt, err := t.game.Validate(input)
	if err != nil {
		return nil, err
	}
	t.inputs[player], t.attempts[player] = input, attempt
	t.notifier.Notify()
	return attempt, nil
}

func (t *Table) Ready(player string) {
	t.mu.Lock()
	defer t.mu.Unlock()
	if t.phase == TableResults && t.seated(player) {
		t.ready[player] = true
		t.startIfAllReady()
	}
	t.notifier.Notify()
}

func (t *Table) SetPresent(player string, present bool) {
	t.mu.Lock()
	defer t.mu.Unlock()
	t.absent[player] = !present
	if t.phase == TableResults {
		t.startIfAllReady()
	}
	t.notifier.Notify()
}

func (t *Table) Close() {
	t.mu.Lock()
	defer t.mu.Unlock()
	if t.closed {
		return
	}
	t.closed = true
	t.turn.Sync(false, nil)
	t.notifier.Close()
}

func startTable(g *Game, players []string, turnTime time.Duration, clock timing.Clock, live bool, l events.Listener) *Table {
	t := &Table{game: g, players: players, ready: map[string]bool{}, absent: map[string]bool{}, totals: map[string]int{},
		turn: timing.NewCountdown(clock), turnTime: turnTime, live: live, notifier: notify.New(l)}
	t.mu.Lock()
	defer t.mu.Unlock()
	t.nextRound()
	return t
}

func (t *Table) checkPlaying(player string) error {
	switch {
	case !t.seated(player):
		return errNoPlayer
	case t.phase != TablePlaying:
		return errRoundEnded
	}
	return nil
}

func (t *Table) seated(player string) bool {
	for _, p := range t.players {
		if p == player {
			return true
		}
	}
	return false
}

func (t *Table) startIfAllReady() {
	for _, p := range t.players {
		if !t.ready[p] && !t.absent[p] {
			return
		}
	}
	t.nextRound()
}

func (t *Table) nextRound() {
	t.inputs, t.attempts, t.ready = map[string]string{}, map[string]*Attempt{}, map[string]bool{}
	draw, err := t.game.DrawRack()
	if err != nil || draw.GameOver {
		t.phase = TableFinished
		return
	}
	t.phase = TablePlaying
	t.turn.Restart(t.turnTime)
	t.turn.Sync(true, t.onTurnEnd)
}

func (t *Table) onTurnEnd() {
	t.mu.Lock()
	defer t.mu.Unlock()
	if t.closed || t.phase != TablePlaying || t.turn.Left() > 0 {
		return
	}
	t.closeRound()
	t.notifier.Notify()
}

func (t *Table) closeRound() {
	plays := t.judgeAll()
	master, err := t.game.closeWith(plays[0])
	if err != nil {
		return
	}
	t.record(master, plays)
	t.phase = TableResults
	if t.game.Status().Over {
		t.phase = TableFinished
	}
}

func (t *Table) judgeAll() []core.Play {
	plays := make([]core.Play, len(t.players))
	for i, p := range t.players {
		plays[i] = t.game.judge(t.inputs[p])
	}
	return plays
}

func (t *Table) record(master *Turn, plays []core.Play) {
	t.totals[masterName] += master.MasterScore
	round := &tableRound{Number: master.Number, Rack: master.Rack,
		Plays: []*TablePlay{{Name: masterName, Kind: core.KindPlay, Coords: master.MasterCoords, Tiles: master.MasterTiles,
			Points: master.MasterScore, Total: t.totals[masterName]}}}
	for i, p := range t.players {
		round.Plays = append(round.Plays, t.scored(p, plays[i]))
	}
	t.rounds = append(t.rounds, round)
}

func (t *Table) scored(player string, play core.Play) *TablePlay {
	points := 0
	if play.Kind == core.KindPlay {
		points = play.Score
	}
	t.totals[player] += points
	return &TablePlay{Name: player, Kind: play.Kind, Coords: play.Coords, Tiles: play.Tiles, Points: points, Total: t.totals[player]}
}

func (d *Game) currentRack() string {
	d.mu.Lock()
	defer d.mu.Unlock()
	return d.rack
}

func (d *Game) judge(input string) core.Play {
	d.mu.Lock()
	defer d.mu.Unlock()
	if input == "" {
		return core.Play{Kind: core.KindTimeout}
	}
	play, _, err := d.humanPlay(input)
	if err != nil {
		return core.Play{Kind: core.KindTimeout}
	}
	return play
}

func (d *Game) closeWith(play core.Play) (*Turn, error) {
	d.mu.Lock()
	defer d.mu.Unlock()
	if !d.inTurn() {
		return nil, errNoTurn
	}
	return d.closeTurn(play, play.Score)
}
