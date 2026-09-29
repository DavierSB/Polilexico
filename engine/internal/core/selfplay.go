package core

import (
	"github.com/domino14/macondo/ai/bot"
	"github.com/domino14/macondo/game"
	pb "github.com/domino14/macondo/gen/api/proto/macondo"
	"github.com/domino14/macondo/move"
)

// Tope de turnos de una partida de HastyBot contra si mismo, por si no terminara.
const maxSelfPlayTurns = 200

// SelfPlay es una partida de HastyBot contra si mismo, turno a turno: la de adorno, la
// busqueda de Scrabble Sprint y la de Finales.
type SelfPlay struct {
	Game  *game.Game
	Bot   *bot.BotTurnPlayer
	turns int
}

// NewSelfPlay empieza una partida nueva, ya repartida.
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

// Best: la jugada de HastyBot para el jugador en turno. GenerateMoves reutiliza sus buffers:
// vale hasta la siguiente llamada.
func (s *SelfPlay) Best() *move.Move {
	return s.Bot.GenerateMoves(1)[0]
}

// Play juega m para el jugador en turno.
func (s *SelfPlay) Play(m *move.Move) error {
	s.turns++
	return s.Game.PlayMove(m, true, 0)
}

// Over: la partida termino (o llego al tope de turnos).
func (s *SelfPlay) Over() bool {
	return s.Game.Playing() == pb.PlayState_GAME_OVER || s.turns >= maxSelfPlayTurns
}

// CopyMove: una copia de m que sobrevive a la siguiente generacion de jugadas.
func CopyMove(m *move.Move) *move.Move {
	c := new(move.Move)
	c.CopyFrom(m)
	return c
}
