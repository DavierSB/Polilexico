package preendgame

import (
	"github.com/domino14/macondo/ai/bot"
	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
)

type preBot struct {
	game    *game.Game
	config  Config
	hasty   *bot.BotTurnPlayer
	spots   *spotFinder
	letters letters
}

func newBot(g *game.Game, c Config) (*preBot, error) {
	hasty, err := core.NewBot(g, core.DefaultBot)
	if err != nil {
		return nil, err
	}
	ls, err := newLetters()
	if err != nil {
		return nil, err
	}
	spots, err := newSpotFinder(g.Board(), ls.q)
	return &preBot{game: g, config: c, hasty: hasty, spots: spots, letters: ls}, err
}

func (b *preBot) move(drawBase int) *move.Move {
	s := b.situation(drawBase)
	top := b.hasty.GenerateMoves(1)[0]
	if s.where == onBoard {
		return top
	}
	plays := b.hasty.GenerateMoves(allPlays)
	if m := b.bingoOrPass(s, plays); m != nil {
		return m
	}
	return b.choose(top, plays, s)
}

func (b *preBot) bingoOrPass(s situation, plays []*move.Move) *move.Move {
	g := b.game
	if s.where != unseen || s.bag > game.RackTileLimit || !ahead(g, g.PlayerOnTurn(), s.unseen) {
		return nil
	}
	if bingo := bestBingo(plays); bingo != nil && b.bingoWins(s, bingo) {
		return bingo
	}
	if b.winsByPassing(s) {
		return move.NewPassMove(g.RackFor(g.PlayerOnTurn()).TilesOn(), g.Alphabet())
	}
	return nil
}
