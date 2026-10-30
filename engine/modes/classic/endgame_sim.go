package classic

import (
	"math/rand"

	"lexico/engine/internal/core"
)

type simulation struct {
	play  *core.SelfPlay
	turns []simTurn
}

type simTurn struct {
	player int
	record turnRecord
	play   core.Play
	totals [2]int
}

func newSimulation() (*simulation, error) {
	play, err := core.NewSelfPlay()
	if err != nil {
		return nil, err
	}
	return &simulation{play: play}, nil
}

func (sim *simulation) reached(maxBag int) bool {
	return sim.bag() <= maxBag
}

func (sim *simulation) bag() int {
	return sim.play.Game.Bag().TilesRemaining()
}

func (sim *simulation) over() bool {
	return sim.play.Over()
}

func (sim *simulation) lead() int {
	g := sim.play.Game
	onTurn := g.PlayerOnTurn()
	return g.PointsFor(onTurn) - g.PointsFor(1-onTurn)
}

func (sim *simulation) step() error {
	g := sim.play.Game
	top := sim.play.Bot.GenerateMoves(reviewCandidates)
	m := core.CopyMove(top[0])
	play := core.PlayOf(m)
	turn := simTurn{player: g.PlayerOnTurn(), play: play,
		record: turnRecordOf(g, m, play, core.Candidates(top), len(sim.turns)+1)}
	if err := sim.play.Play(m); err != nil {
		return err
	}
	turn.totals = [2]int{g.PointsFor(0), g.PointsFor(1)}
	sim.turns = append(sim.turns, turn)
	return nil
}

func (sim *simulation) game() (*Game, error) {
	return sim.seated(sim.play.Game.PlayerOnTurn(), humanName, core.DefaultBot, newGameLog(humanName, core.DefaultBot))
}

func (sim *simulation) localGame(first, second string, starter int) (*Game, error) {
	log := newGameLog(first, second)
	log.Local = true
	return sim.seated(localSide(sim.play.Game.PlayerOnTurn(), starter), first, second, log)
}

func (sim *simulation) seated(humanIdx int, human, other string, log *gameLog) (*Game, error) {
	g := sim.play.Game
	first, second := playerNames(humanIdx, human, other)
	if err := core.RenamePlayers(g, first, second); err != nil {
		return nil, err
	}
	log.Mode, log.StartTurn = ModeEndgame, len(sim.turns)
	c, err := newGame(g, other, humanIdx, log)
	if err != nil {
		return nil, err
	}
	for _, t := range sim.turns {
		c.addPastTurn(t)
	}
	return c, nil
}

func (c *Game) addPastTurn(t simTurn) {
	byHuman := t.player == c.humanIdx
	t.record.PlayerName = c.playerName(byHuman)
	c.log.Turns = append(c.log.Turns, t.record)
	c.moves = append(c.moves, moveOf(t.play, byHuman, t.totals[c.humanIdx], t.totals[c.botIdx()]))
}

func playerNames(humanIdx int, human, other string) (string, string) {
	if humanIdx == 0 {
		return human, other
	}
	return other, human
}

func localSide(onTurn, starter int) int {
	if starter == StarterBot || starter != StarterHuman && rand.Intn(2) == 0 {
		return 1 - onTurn
	}
	return onTurn
}
