package core

import (
	"fmt"
	"strings"

	"github.com/domino14/macondo/board"
	"github.com/domino14/macondo/game"
)

func RenderBoard(g *game.Game) string {
	n := g.Board().Dim()
	var sb strings.Builder
	writeHeader(&sb, n)
	for row := 0; row < n; row++ {
		writeRow(&sb, g, row)
	}
	writeRule(&sb, n)
	return sb.String()
}

func BoardText(g *game.Game) string {
	n := g.Board().Dim()
	squares := make([]string, 0, n*n)
	for row := 0; row < n; row++ {
		for col := 0; col < n; col++ {
			squares = append(squares, squareText(g, row, col))
		}
	}
	return strings.Join(squares, " ")
}

func squareText(g *game.Game, row, col int) string {
	if !g.Board().HasLetter(row, col) {
		return "."
	}
	return withoutBrackets(g.Board().GetLetter(row, col).UserVisible(g.Alphabet(), false))
}

func writeHeader(sb *strings.Builder, n int) {
	sb.WriteString("\n    ")
	for col := 1; col <= n; col++ {
		fmt.Fprintf(sb, "%-2d ", col)
	}
	sb.WriteString("\n")
	writeRule(sb, n)
}

func writeRow(sb *strings.Builder, g *game.Game, row int) {
	fmt.Fprintf(sb, "  %c|", 'A'+row)
	for col := 0; col < g.Board().Dim(); col++ {
		sb.WriteString(g.Board().SQDisplayStr(row, col, g.Alphabet(), board.ColorSupport))
		sb.WriteString(" ")
	}
	sb.WriteString("|\n")
}

func writeRule(sb *strings.Builder, n int) {
	sb.WriteString("    " + strings.Repeat("-", n*3) + "\n")
}
