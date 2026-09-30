package classic

import (
	"errors"

	"github.com/domino14/macondo/move"
	"github.com/domino14/macondo/turnplayer"

	"lexico/engine/internal/core"
)

const reviewCandidates = 15

var (
	errNotYourTurn = errors.New("no es tu turno")
	errNotBotTurn  = errors.New("no es el turno del bot")
)

func (c *Game) Play(input string) (*Move, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if !c.humanToMove() {
		return nil, errNotYourTurn
	}
	m, play, err := c.humanMove(input)
	if err != nil {
		return nil, err
	}
	return c.apply(m, play, c.candidates())
}

func (c *Game) PlayBot() (*Move, error) {
	choice, err := c.chooseBotMove()
	if err != nil {
		return nil, err
	}
	return c.applyBotMove(choice)
}

type botChoice struct {
	move       *move.Move
	candidates []core.Candidate
	turn       int
}

func (c *Game) chooseBotMove() (*botChoice, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if !c.botToMove() {
		return nil, errNotBotTurn
	}
	candidates := c.candidates()
	m := new(move.Move)
	m.CopyFrom(c.bot.GenerateMoves(1)[0])
	return &botChoice{move: m, candidates: candidates, turn: len(c.moves)}, nil
}

func (c *Game) applyBotMove(choice *botChoice) (*Move, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if !c.botToMove() || choice.turn != len(c.moves) {
		return nil, errNotBotTurn
	}
	return c.apply(choice.move, core.PlayOf(choice.move), choice.candidates)
}

func (c *Game) LoseOnTime() {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.over() {
		return
	}
	c.lostOnTime = true
	c.finish()
}

func (c *Game) humanMove(input string) (*move.Move, core.Play, error) {
	m, err := core.ParseInput(c.g, c.humanIdx, input)
	if invalid, ok := core.AsInvalidWords(err); ok && c.invalidLosesTurn {
		return c.loseTurn(invalid.Play)
	}
	if err != nil {
		return nil, core.Play{}, err
	}
	return m, core.PlayOf(m), nil
}

func (c *Game) loseTurn(attempt core.Play) (*move.Move, core.Play, error) {
	tp := &turnplayer.BaseTurnPlayer{Game: c.g}
	m, err := tp.NewPassMove(c.humanIdx)
	return m, attempt, err
}

func (c *Game) apply(m *move.Move, play core.Play, candidates []core.Candidate) (*Move, error) {
	byHuman := c.humanToMove()
	record := c.newTurnRecord(m, play, candidates)
	c.g.SetMaxScorelessTurns(noScorelessEnd)
	if err := c.g.PlayMove(m, true, 0); err != nil {
		return nil, err
	}
	c.dropOutBonus()
	c.log.Turns = append(c.log.Turns, record)
	played := c.newMove(play, byHuman)
	c.moves = append(c.moves, played)
	c.settleEnding()
	c.finishIfOver()
	return played, nil
}

func (c *Game) candidates() []core.Candidate {
	return core.Candidates(c.bot.GenerateMoves(reviewCandidates))
}

func (c *Game) newMove(p core.Play, byHuman bool) *Move {
	return moveOf(p, byHuman, c.points(true), c.points(false))
}

func moveOf(p core.Play, byHuman bool, humanTotal, botTotal int) *Move {
	if !byHuman {
		p = hideExchange(p)
	}
	return &Move{ByHuman: byHuman, Kind: p.Kind, Coords: p.Coords, Tiles: p.Tiles,
		TileCount: p.TileCount, Score: p.Score, HumanTotal: humanTotal, BotTotal: botTotal}
}

func hideExchange(p core.Play) core.Play {
	if p.Kind == core.KindExchange {
		p.Tiles = ""
	}
	return p
}
