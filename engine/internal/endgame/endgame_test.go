package endgame

import (
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
	"lexico/engine/internal/testenv"
)

const twoBlanks = "2DE[RR]UBIE6/1TEX11/2N12/2GEA6M3/2U1T6A3/2E1OC5N3/2O1[CH]A1D3I3/3YERNO3J2R/3ES2CAUCASEA/1D1R3E6F/HI1B3A3P2E/AL1E3V1H1E2Z/RUÑAN1COPANDO2/TI1I3S1[LL]1I3/A2S6MOGOL LOQRSTU/??ADLOS 341/437"

const noBlanks = "7ARAGUEÑO/7T7/2DEBOCO7/1DiX2AJ2MIE2/6PERDED3/4G1E8/4U1A8/3CAMbETA5/4T1A7C/1H2A[LL]I1PRENSIL/1U2Z1SOLETA2O/BIRLON1VA5N/1N2S2UY5Q/1[CH]5LO5U/FA[RR]O3O6I DEEHIRS/ACENSS 419/447"

const lastTiles = "TOlUENOS7/I4ACETATO3/L14/O9D4/SE8E4/AJ3G4S4/SEDUCIAN2R1H2/5L1ALIAMOS1/5E4B1L1V/5Y1HU[LL]A1G1I/6DEFERIA1E/3Q3[RR]2EDREN/2BUXACEA3O1T/3I3Ñ4N1A/PRODuZCA6N [CH]MOOPRS/U 463/555"

func TestPlayoutLeavesPositionUntouched(t *testing.T) {
	pos, err := NewPosition(endgameOf(t, noBlanks))
	if err != nil {
		t.Fatal(err)
	}
	player, spread, tiles := pos.OnTurn(), pos.Spread(0), pos.RackSize(0)+pos.RackSize(1)
	pos.Playout(player)
	if pos.OnTurn() != player || pos.Spread(0) != spread || pos.RackSize(0)+pos.RackSize(1) != tiles {
		t.Fatal("el playout no deshizo sus jugadas")
	}
}

func TestSearchKeepsBudgetWithTwoBlanks(t *testing.T) {
	g := endgameOf(t, twoBlanks)
	c := Default
	c.Budget = time.Second
	r, err := Search(g, c)
	if err != nil {
		t.Fatal(err)
	}
	if r.Move == nil || r.Elapsed > 1500*time.Millisecond {
		t.Fatalf("resultado inesperado: %+v", r)
	}
	playable(t, g, r.Move)
}

func TestSearchFinishesShortEndgameEarly(t *testing.T) {
	r, err := Search(endgameOf(t, lastTiles), Default)
	if err != nil {
		t.Fatal(err)
	}
	if r.Depth != Default.MaxDepth || r.Elapsed > Default.Budget/2 {
		t.Fatalf("un final de 8 fichas debería llegar al fondo enseguida: %+v", r)
	}
}

func TestAnalysisRanksLinesBySpread(t *testing.T) {
	g := endgameOf(t, noBlanks)
	a, err := Analyze(g, Default, 5, nil)
	if err != nil {
		t.Fatal(err)
	}
	if len(a.Lines) != 5 || a.Depth < 2 {
		t.Fatalf("análisis inesperado: %d líneas, nivel %d", len(a.Lines), a.Depth)
	}
	for i := 1; i < len(a.Lines); i++ {
		if a.Lines[i].Spread > a.Lines[i-1].Spread {
			t.Fatalf("líneas desordenadas: %d antes que %d", a.Lines[i-1].Spread, a.Lines[i].Spread)
		}
	}
}

func TestAnalysisLinesReachTheirSpread(t *testing.T) {
	g := endgameOf(t, lastTiles)
	a, err := Analyze(g, Default, 4, nil)
	if err != nil {
		t.Fatal(err)
	}
	for _, l := range a.Lines {
		if got := finalSpread(t, g, l); got != l.Spread {
			t.Fatalf("%s: la línea termina en %d, no en %d", l.Move.ShortDescription(), got, l.Spread)
		}
	}
}

func TestAnalysisKeepsRequestedMove(t *testing.T) {
	g := endgameOf(t, noBlanks)
	isPass := func(m *move.Move) bool { return m.Action() == move.MoveTypePass }
	a, err := Analyze(g, Default, 3, isPass)
	if err != nil {
		t.Fatal(err)
	}
	for _, l := range a.Lines {
		if isPass(l.Move) {
			return
		}
	}
	t.Fatal("falta el pase pedido")
}

func finalSpread(t *testing.T, g *game.Game, l Line) int {
	pos, err := NewPosition(g)
	if err != nil {
		t.Fatal(err)
	}
	player := pos.OnTurn()
	for _, m := range append([]*move.Move{l.Move}, l.Reply...) {
		if err := pos.Play(m); err != nil {
			t.Fatalf("jugada ilegal %s: %v", m.ShortDescription(), err)
		}
	}
	if !pos.Over() {
		t.Fatalf("%s: la línea no llega al final", l.Move.ShortDescription())
	}
	return pos.Spread(player)
}

func playable(t *testing.T, g *game.Game, m *move.Move) {
	if err := g.Copy().PlayMove(m, false, 0); err != nil {
		t.Fatalf("jugada ilegal %s: %v", m.ShortDescription(), err)
	}
}

func endgameOf(t *testing.T, cgp string) *game.Game {
	testenv.Init(t)
	fields := strings.Fields(cgp)
	rows, err := core.ParseBoard(boardText(fields[0]))
	if err != nil {
		t.Fatal(err)
	}
	g, err := core.NewEndgame(rows, strings.Split(fields[1], "/")[0], scores(t, fields[2]))
	if err != nil {
		t.Fatal(err)
	}
	return g
}

func scores(t *testing.T, text string) [2]int {
	parts := strings.Split(text, "/")
	first, err1 := strconv.Atoi(parts[0])
	second, err2 := strconv.Atoi(parts[1])
	if err1 != nil || err2 != nil {
		t.Fatalf("puntuación ilegible: %s", text)
	}
	return [2]int{first, second}
}

func boardText(rows string) string {
	var squares []string
	for _, row := range strings.Split(rows, "/") {
		squares = append(squares, rowSquares(row)...)
	}
	return strings.Join(squares, " ")
}

func rowSquares(row string) []string {
	var out []string
	for rest := row; rest != ""; {
		token, n := nextToken(rest)
		out = append(out, token...)
		rest = rest[n:]
	}
	return out
}

func nextToken(rest string) ([]string, int) {
	if rest[0] >= '0' && rest[0] <= '9' {
		n := 1
		for n < len(rest) && rest[n] >= '0' && rest[n] <= '9' {
			n++
		}
		empty, _ := strconv.Atoi(rest[:n])
		return strings.Fields(strings.Repeat(". ", empty)), n
	}
	if rest[0] == '[' {
		end := strings.IndexByte(rest, ']')
		return []string{rest[1:end]}, end + 1
	}
	r := []rune(rest)[0]
	return []string{string(r)}, len(string(r))
}
