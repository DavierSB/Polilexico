package engine

import (
	"strings"

	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
)

func Init(dataDir, savesDir string) error {
	return core.Init(dataDir, savesDir)
}

func IsValidWord(word string) (bool, error) {
	g, err := core.NewGame("a", "b")
	if err != nil {
		return false, err
	}
	w, err := tilemapping.ToMachineWord(normalizeWord(word), g.Alphabet())
	if err != nil {
		return false, err
	}
	return core.IsWord(g, w), nil
}

func BestMoves(board, rack string, n int) (string, error) {
	g, err := gameFromPosition(board, rack)
	if err != nil {
		return "", err
	}
	hasty, err := core.NewBot(g, core.DefaultBot)
	if err != nil {
		return "", err
	}
	return core.JSON(core.Candidates(hasty.GenerateMoves(n)))
}

func PlacementScore(board, placement string) (int, error) {
	g, err := gameFromPosition(board, placedRack(placement))
	if err != nil {
		return 0, err
	}
	m, err := core.PlacementMove(g, 0, placement)
	if err != nil {
		return 0, err
	}
	return m.Score(), nil
}

func normalizeWord(word string) string {
	return core.ExpandEnhe(strings.ToUpper(strings.TrimSpace(word)))
}
