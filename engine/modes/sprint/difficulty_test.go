package sprint

import (
	"testing"
	"time"

	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
	"lexico/engine/internal/testenv"
)

func TestSameFamilyLooksAtTheFirstFourLetters(t *testing.T) {
	word := func(letters ...tilemapping.MachineLetter) []tilemapping.MachineLetter { return letters }
	cases := []struct {
		a, b []tilemapping.MachineLetter
		same bool
	}{
		{word(3, 1, 13, 20, 1, 19), word(3, 1, 13, 20, 1, 19, 5, 21), true},
		{word(3, 1, 13, 20, 1, 19), word(3, 1, 13, 21, 1, 19), false},
	}
	for _, c := range cases {
		if sameFamily(c.a, c.b) != c.same {
			t.Errorf("%v / %v: misma familia debería ser %v", c.a, c.b, c.same)
		}
	}
}

func TestHunterRespectsTheDifficulty(t *testing.T) {
	testenv.Init(t)
	for _, difficulty := range []string{DifficultyEasy, DifficultyHard} {
		h := startHunter(func() int { return 1 }, difficulty)
		p := firstPuzzle(t, h)
		h.stop()
		if bingos := puzzleBingos(t, p); !fits(difficulty, p.g.Board(), bingos) {
			t.Fatalf("%s: %d familias en %d scrabbles", difficulty, familyCount(p.g.Board(), bingos), len(bingos))
		}
	}
}

func firstPuzzle(t *testing.T, h *hunter) *puzzle {
	t.Helper()
	select {
	case f := <-h.found:
		if f.err != nil {
			t.Fatal(f.err)
		}
		return f.puzzle
	case <-time.After(searchTimeout):
		t.Fatal("no encontró ninguna mano")
	}
	return nil
}

func puzzleBingos(t *testing.T, p *puzzle) []*move.Move {
	t.Helper()
	gen, err := core.NewMaster(p.g)
	if err != nil {
		t.Fatal(err)
	}
	var out []*move.Move
	for _, m := range gen.GenAll(p.g.RackFor(p.g.PlayerOnTurn()), false) {
		if isBingo(m) {
			out = append(out, core.CopyMove(m))
		}
	}
	return out
}
