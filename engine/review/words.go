package review

import (
	"strings"
	"unicode/utf8"

	"lexico/engine/internal/core"
)

// Las palabras de tus colocaciones, para Game.Bingos, BestWord y LongestWord.

// bingoTiles: una colocacion con todas las fichas del atril.
const bingoTiles = 7

// notePlay anota una colocacion tuya sobre el tablero de antes de jugarla.
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

// word: la palabra completa de una colocacion ("H8 CA.A (12 pts)" -> "CASA", con la letra del
// tablero) y cuantas fichas puso; "" si no es una colocacion.
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

// spell escribe la palabra: las fichas nuevas y, en los ".", la letra que ya estaba.
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

// letters: cuantas letras tiene una palabra (la Ñ cuenta una; los digrafos, dos).
func letters(word string) int {
	return utf8.RuneCountInString(word)
}
