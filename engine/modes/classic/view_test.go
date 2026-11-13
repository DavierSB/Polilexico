package classic

import (
	"encoding/json"
	"testing"

	"github.com/domino14/macondo/game"
	pb "github.com/domino14/macondo/gen/api/proto/macondo"
	"github.com/domino14/word-golib/tilemapping"
	"google.golang.org/protobuf/encoding/protojson"

	"lexico/engine/internal/core"
	"lexico/engine/internal/testenv"
	"lexico/engine/internal/timing"
)

func TestViewKeepsWhatEachPlayerSees(t *testing.T) {
	m := viewMatch(t)
	for _, first := range []bool{true, false} {
		mirror := mirrorOf(t, m, first)
		viewer := viewerIdx(m.game, first)
		if got, want := core.RackText(mirror.game.g, viewer), core.RackText(m.game.g, viewer); got != want {
			t.Fatalf("atril propio: %q, quería %q", got, want)
		}
		if got, want := unseenFor(mirror.game.g, viewer), unseenFor(m.game.g, viewer); got != want {
			t.Fatalf("fichas por salir: %q, quería %q", got, want)
		}
		if mirror.game.Board() != m.game.Board() || mirror.game.MoveCount() != m.game.MoveCount() {
			t.Fatal("el tablero o las jugadas no coinciden")
		}
	}
}

func TestViewHidesTheRivalRacks(t *testing.T) {
	m := viewMatch(t)
	for _, first := range []bool{true, false} {
		h := viewHistory(t, m, first)
		rival := 1 - viewerIdx(m.game, first)
		if h.LastKnownRacks[rival] != "" {
			t.Fatalf("se ve el atril del rival: %q", h.LastKnownRacks[rival])
		}
		for _, evt := range h.Events {
			if int(evt.PlayerIndex) == rival && evt.Type == pb.GameEvent_TILE_PLACEMENT_MOVE && tileCount(t, m, evt.Rack) != int(evt.NumTilesFromRack) {
				t.Fatalf("una jugada del rival delata su atril: %q", evt.Rack)
			}
		}
	}
}

func TestViewHidesTheRivalExchange(t *testing.T) {
	m := viewMatch(t)
	h := viewHistory(t, m, true)
	exchange := h.Events[1]
	if exchange.Type != pb.GameEvent_EXCHANGE || tileCount(t, m, exchange.Exchanged) != 3 || exchange.Rack != exchange.Exchanged {
		t.Fatalf("%+v", exchange)
	}
	if mirrorOf(t, m, true).game.MoveAt(1).Tiles != "" {
		t.Fatal("se ven las fichas que cambió el rival")
	}
}

func TestViewHidesPastRecords(t *testing.T) {
	m := viewMatch(t)
	if log := mirrorOf(t, m, true).game.log; len(log.Turns) != 0 {
		t.Fatalf("el registro trae %d turnos", len(log.Turns))
	}
}

func TestViewAtTheEndShowsEverything(t *testing.T) {
	m := viewMatch(t)
	m.Forfeit(true)
	mirror := mirrorOf(t, m, false)
	if r := mirror.game.Result(); r == nil || !r.LostOnTime || core.RackText(mirror.game.g, viewerIdx(m.game, true)) != core.RackText(m.game.g, viewerIdx(m.game, true)) {
		t.Fatalf("%+v", r)
	}
}

func viewMatch(t *testing.T) *Match {
	t.Helper()
	c := newLocalGame(t, StarterHuman)
	playBest(t, c)
	exchangeThree(t, c)
	playBest(t, c)
	playBest(t, c)
	m := startMatch(c, nil, timing.NewFake(), newTestListener(), false)
	t.Cleanup(m.Close)
	return m
}

func playBest(t *testing.T, c *Game) {
	t.Helper()
	if _, err := c.Play(testenv.Input(c.bot.GenerateMoves(1)[0])); err != nil {
		t.Fatal(err)
	}
}

func exchangeThree(t *testing.T, c *Game) {
	t.Helper()
	tiles := tilemapping.MachineWord(c.g.RackFor(c.g.PlayerOnTurn()).TilesOn()[:3])
	if _, err := c.Play("cambiar " + tiles.UserVisible(c.g.Alphabet())); err != nil {
		t.Fatal(err)
	}
}

func mirrorOf(t *testing.T, m *Match, first bool) *Match {
	t.Helper()
	text, err := m.ViewFor(first)
	if err != nil {
		t.Fatal(err)
	}
	mirror, err := LoadMirror(text, true, newTestListener())
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(mirror.Close)
	return mirror
}

func viewHistory(t *testing.T, m *Match, first bool) *pb.GameHistory {
	t.Helper()
	return historyOf(t, func() (string, error) { return m.ViewFor(first) })
}

func historyOf(t *testing.T, view func() (string, error)) *pb.GameHistory {
	t.Helper()
	text, err := view()
	if err != nil {
		t.Fatal(err)
	}
	var sm savedMatch
	var sg savedGame
	h := &pb.GameHistory{}
	if err := json.Unmarshal([]byte(text), &sm); err != nil {
		t.Fatal(err)
	}
	if err := json.Unmarshal([]byte(sm.Game), &sg); err != nil {
		t.Fatal(err)
	}
	if err := protojson.Unmarshal(sg.History, h); err != nil {
		t.Fatal(err)
	}
	return h
}

func viewerIdx(c *Game, first bool) int {
	if first {
		return c.humanIdx
	}
	return 1 - c.humanIdx
}

func unseenFor(g *game.Game, viewer int) string {
	return core.Unseen(g, g.RackFor(1-viewer).TilesOn()...)
}

func tileCount(t *testing.T, m *Match, tiles string) int {
	t.Helper()
	mls, err := tilemapping.ToMachineLetters(tiles, m.game.g.Alphabet())
	if err != nil {
		t.Fatal(err)
	}
	return len(mls)
}

func TestSpectatorSeesBothRacks(t *testing.T) {
	m := viewMatch(t)
	h := historyOf(t, func() (string, error) { return m.SpectatorView() })
	for i, rack := range h.LastKnownRacks {
		if want := core.RackText(m.game.g, i); rack == "" || len(rack) > len(want)+16 {
			t.Fatalf("atril %d: %q, quería %q", i, rack, want)
		}
	}
}

func TestSpectatorAtTheEndGetsTheRecord(t *testing.T) {
	m := viewMatch(t)
	m.Forfeit(true)
	view, err := m.SpectatorView()
	if err != nil {
		t.Fatal(err)
	}
	watched, err := LoadWatch(view, true, newTestListener())
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(watched.Close)
	if got, want := len(watched.game.log.Turns), len(m.game.log.Turns); got == 0 || got != want {
		t.Fatalf("el registro trae %d turnos, quería %d", got, want)
	}
}
