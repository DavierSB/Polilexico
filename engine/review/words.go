package review

import (
	"strings"
	"unicode/utf8"

	"lexico/engine/internal/core"
)

const bingoTiles = 7

func (g *Game) notePlay(board *grid, played core.Candidate) {
	word, placed := board.word(played.Description)
	if word == "" {
		return
	}
	if placed == bingoTiles {
		g.Bingos++
	}
	if played.Score > g.BestWordScore {
		g.BestWord, g.BestWordScore = word, played.Score
	}
	if letters(word) > letters(g.LongestWord) {
		g.LongestWord = word
	}
}

func (b *grid) word(description string) (string, int) {
	fields := strings.Fields(description)
	if len(fields) < 2 {
		return "", 0
	}
	row, col, vertical, err := core.ParseCoords(fields[0])
	if err != nil {
		return "", 0
	}
	return b.spell(row, col, vertical, plain(tokens(fields[1])))
}

func (b *grid) spell(row, col int, vertical bool, tiles []string) (string, int) {
	var sb strings.Builder
	placed := 0
	for i, tile := range tiles {
		if tile != "." {
			placed++
		}
		sb.WriteString(strings.ToUpper(b.letterAt(row, col, vertical, i, tile)))
	}
	return sb.String(), placed
}

func (b *grid) letterAt(row, col int, vertical bool, i int, tile string) string {
	if tile != "." {
		return tile
	}
	if vertical {
		row += i
	} else {
		col += i
	}
	if row >= size || col >= size {
		return ""
	}
	return b[row][col]
}

func letters(word string) int {
	return utf8.RuneCountInString(word)
}
