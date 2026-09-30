package engine

import (
	"strings"
	"testing"

	"lexico/engine/internal/testenv"
)

var emptyBoard = strings.TrimSpace(strings.Repeat(". ", boardSize*boardSize))

func TestIsValidWord(t *testing.T) {
	testenv.Init(t)
	for word, want := range map[string]bool{"CASA": true, "AÑO": true, "A[N]O": true, "CASAQ": false} {
		if got, err := IsValidWord(word); err != nil || got != want {
			t.Errorf("%s: %v %v", word, got, err)
		}
	}
}

func TestBestMovesOnEmptyBoard(t *testing.T) {
	testenv.Init(t)
	moves, err := BestMoves(emptyBoard, "AEIRST?", 5)
	if err != nil || !strings.Contains(moves, `"description"`) {
		t.Fatalf("%s %v", moves, err)
	}
	t.Log(moves)
}

func TestBestMovesWithDigraph(t *testing.T) {
	testenv.Init(t)
	board := boardWith(map[int]string{7*15 + 7: "C", 7*15 + 8: "A", 7*15 + 9: "S", 7*15 + 10: "A", 8*15 + 7: "CH"})
	moves, err := BestMoves(board, "EIORSTU", 3)
	if err != nil {
		t.Fatal(err)
	}
	t.Log(moves)
}

func TestBestMovesRejectsImpossiblePosition(t *testing.T) {
	testenv.Init(t)
	board := boardWith(map[int]string{0: "Z", 1: "Z", 2: "Z"})
	if _, err := BestMoves(board, "AEIOU", 3); err == nil {
		t.Fatal("aceptó tres zetas")
	}
}

func boardWith(tiles map[int]string) string {
	squares := strings.Fields(emptyBoard)
	for i, tile := range tiles {
		squares[i] = tile
	}
	return strings.Join(squares, " ")
}

func TestPlacementScore(t *testing.T) {
	testenv.Init(t)
	for placement, want := range map[string]int{"H8 CASA": 12, "8H CASA": 12, "H8 CAsA": 10, "H7 [CH]E": 12} {
		if got, err := PlacementScore(emptyBoard, placement); err != nil || got != want {
			t.Errorf("%s: %d %v, se esperaba %d", placement, got, err, want)
		}
	}
}

func TestPlacementScoreThroughTiles(t *testing.T) {
	testenv.Init(t)
	board := boardWith(map[int]string{7*15 + 7: "C", 7*15 + 8: "A", 7*15 + 9: "S", 7*15 + 10: "A"})
	if got, err := PlacementScore(board, "H8 CASAS"); err != nil || got != 8 {
		t.Errorf("%d %v", got, err)
	}
}

func TestPlacementScoreRejectsIllegalPlacements(t *testing.T) {
	testenv.Init(t)
	for _, placement := range []string{"A1 CASA", "H8", ""} {
		if _, err := PlacementScore(emptyBoard, placement); err == nil {
			t.Errorf("%q: aceptada", placement)
		}
	}
}
