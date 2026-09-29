// Package demo juega partidas de HastyBot contra si mismo: de adorno en la pantalla de inicio
// y como partida a recordar en "¿Cuántas recuerdas?".
package demo

import (
	"github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
)

// ScoredPlacement es una colocacion de la partida con los puntos que hizo.
type ScoredPlacement struct {
	Placement string `json:"placement"` // "H8 CA.A"
	Score     int    `json:"score"`
}

// Play juega una partida entera y devuelve sus colocaciones en orden, como JSON
// ["H8 CA.A", ...] (notacion FISE; los pases y cambios no se incluyen).
func Play() (string, error) {
	scored, err := playGame()
	if err != nil {
		return "", err
	}
	placements := make([]string, len(scored))
	for i, s := range scored {
		placements[i] = s.Placement
	}
	return core.JSON(placements)
}

// PlayWithScores es Play con los puntos de cada colocacion, como JSON
// [{"placement":"H8 CA.A","score":12}, ...].
func PlayWithScores() (string, error) {
	scored, err := playGame()
	if err != nil {
		return "", err
	}
	return core.JSON(scored)
}

func playGame() ([]ScoredPlacement, error) {
	s, err := core.NewSelfPlay()
	if err != nil {
		return nil, err
	}
	return selfPlay(s)
}

func selfPlay(s *core.SelfPlay) ([]ScoredPlacement, error) {
	placements := []ScoredPlacement{}
	for !s.Over() {
		m := s.Best()
		if m.Action() == move.MoveTypePlay {
			placements = append(placements, ScoredPlacement{placement(m), m.Score()})
		}
		if err := s.Play(m); err != nil {
			return nil, err
		}
	}
	return placements, nil
}

func placement(m *move.Move) string {
	return core.MoveCoords(m) + " " + m.TilesString()
}
