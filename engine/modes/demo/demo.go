package demo

import (
	"github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
)

type ScoredPlacement struct {
	Placement string `json:"placement"`
	Score     int    `json:"score"`
}

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
