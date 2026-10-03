package sprint

import (
	"errors"
	"slices"

	"github.com/domino14/macondo/board"
	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
)

const (
	DifficultyEasy   = "easy"
	DifficultyNormal = "normal"
	DifficultyHard   = "hard"
)

const familyPrefix = 4

var errDifficulty = errors.New("dificultad desconocida")

func validDifficulty(d string) bool {
	return d == DifficultyEasy || d == DifficultyNormal || d == DifficultyHard
}

func fits(difficulty string, b *board.GameBoard, bingos []*move.Move) bool {
	switch difficulty {
	case DifficultyEasy:
		return familyCount(b, bingos) > 1
	case DifficultyHard:
		return familyCount(b, bingos) == 1
	}
	return true
}

func familyCount(b *board.GameBoard, bingos []*move.Move) int {
	var families [][]tilemapping.MachineLetter
	for _, m := range bingos {
		word := core.MainWord(b, m)
		if !slices.ContainsFunc(families, func(f []tilemapping.MachineLetter) bool { return sameFamily(f, word) }) {
			families = append(families, word)
		}
	}
	return len(families)
}

func sameFamily(a, b []tilemapping.MachineLetter) bool {
	return slices.Equal(prefix(a), prefix(b))
}

func prefix(word []tilemapping.MachineLetter) []tilemapping.MachineLetter {
	return word[:min(len(word), familyPrefix)]
}
