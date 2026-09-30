package core

import (
	"github.com/domino14/macondo/ai/bot"
	"github.com/domino14/macondo/game"
	pb "github.com/domino14/macondo/gen/api/proto/macondo"
	"github.com/domino14/macondo/move"
)

const maxSelfPlayTurns = 200

type SelfPlay struct {
	Game  *game.Game
	Bot   *bot.BotTurnPlayer
	turns int
}

func NewSelfPlay() (*SelfPlay, error) {
	g, err := NewGame("A", "B")
	if err != nil {
		return nil, err
	}
	g.StartGame()
	hasty, err := NewBot(g, DefaultBot)
	if err != nil {
		return nil, err
	}
	return &SelfPlay{Game: g, Bot: hasty}, nil
}

func (s *SelfPlay) Best() *move.Move {
	return s.Bot.GenerateMoves(1)[0]
}

func (s *SelfPlay) Play(m *move.Move) error {
	s.turns++
	return s.Game.PlayMove(m, true, 0)
}

func (s *SelfPlay) Over() bool {
	return s.Game.Playing() == pb.PlayState_GAME_OVER || s.turns >= maxSelfPlayTurns
}

func CopyMove(m *move.Move) *move.Move {
	c := new(move.Move)
	c.CopyFrom(m)
	return c
}
