package classic

import (
	"lexico/engine/internal/core"
)

// simulation es una partida de HastyBot contra si mismo con cada turno anotado, para
// entregarla como partida clasica si llega al final buscado. Hasta entonces no se sabe que
// jugador seras tu: el que tenga el turno en ese momento.
type simulation struct {
	play  *core.SelfPlay
	turns []simTurn
}

// simTurn es un turno jugado: quien, su registro (sin nombre aun) y el marcador tras el.
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

// reached: la bolsa ya bajo a maxBag fichas o menos.
func (sim *simulation) reached(maxBag int) bool {
	return sim.play.Game.Bag().TilesRemaining() <= maxBag
}

func (sim *simulation) over() bool {
	return sim.play.Over()
}

// lead: los puntos del jugador en turno menos los de su rival.
func (sim *simulation) lead() int {
	g := sim.play.Game
	onTurn := g.PlayerOnTurn()
	return g.PointsFor(onTurn) - g.PointsFor(1-onTurn)
}

// step juega la mejor jugada de HastyBot y la anota con las candidatas del turno, para la
// revision. Pedir las candidatas no cuesta mas: HastyBot las genera todas igualmente.
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

// game: la partida simulada como partida clasica, contigo en el jugador en turno.
func (sim *simulation) game() (*Game, error) {
	g := sim.play.Game
	humanIdx := g.PlayerOnTurn()
	first, second := playerNames(humanIdx)
	if err := core.RenamePlayers(g, first, second); err != nil {
		return nil, err
	}
	c, err := newGame(g, core.DefaultBot, humanIdx, newGameLog(core.DefaultBot))
	if err != nil {
		return nil, err
	}
	c.log.Mode, c.log.StartTurn = ModeEndgame, len(sim.turns)
	for _, t := range sim.turns {
		c.addPastTurn(t)
	}
	return c, nil
}

// addPastTurn anota un turno ya jugado, ahora que se sabe quien eres.
func (c *Game) addPastTurn(t simTurn) {
	byHuman := t.player == c.humanIdx
	t.record.PlayerName = c.playerName(byHuman)
	c.log.Turns = append(c.log.Turns, t.record)
	c.moves = append(c.moves, moveOf(t.play, byHuman, t.totals[c.humanIdx], t.totals[c.botIdx()]))
}

// playerNames: los nombres de los jugadores 0 y 1, siendo tu humanIdx.
func playerNames(humanIdx int) (string, string) {
	if humanIdx == 0 {
		return humanName, core.DefaultBot
	}
	return core.DefaultBot, humanName
}
