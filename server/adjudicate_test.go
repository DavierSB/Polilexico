package main

import (
	"encoding/json"
	"strings"
	"testing"
	"time"

	"github.com/coder/websocket"

	"lexico/engine"
	"lexico/engine/modes/classic"
)

type quiet struct{}

func (quiet) OnChange() {}

func TestAdjudicationAwardsTheLeader(t *testing.T) {
	adjudicateAfter = 0
	defer func() { adjudicateAfter = 3 * time.Minute }()
	srv := newTestServer(t)
	onTurn, waiting, state := startedGame(t, srv)
	play(t, onTurn, state)
	waiting.conn.Close(websocket.StatusNormalClosure, "")
	onTurn.waitUntil(func(m message) bool { return m.T == "rival" && !m.Online })
	onTurn.send(message{T: "adjudicate"})
	onTurn.waitUntil(func(m message) bool { return m.T == "state" && strings.Contains(m.Save, `LostOnTime\":true`) })
	waitStats(t, srv, onTurn.name, onTurn.country, "win")
}

func TestAdjudicationIgnoresAPlayThenLeave(t *testing.T) {
	adjudicateAfter = 0
	defer func() { adjudicateAfter = 3 * time.Minute }()
	srv := newTestServer(t)
	first, second, state := startedGame(t, srv)
	play(t, first, state)
	play(t, second, second.waitFor("state"))
	second.conn.Close(websocket.StatusNormalClosure, "")
	first.waitUntil(func(m message) bool { return m.T == "rival" && !m.Online })
	first.send(message{T: "adjudicate"})
	first.waitUntil(func(m message) bool { return m.T == "state" && strings.Contains(m.Save, `LostOnTime\":true`) })
	waitStats(t, srv, first.name, first.country, "win")
}

func play(t *testing.T, c *testClient, state message) {
	t.Helper()
	input := bestPlay(t, state)
	c.send(message{T: "move", Input: input})
	if m := c.waitUntil(func(m message) bool { return m.T == "state" || m.T == "rejected" }); m.T == "rejected" {
		t.Fatalf("jugada %q rechazada: %s", input, m.Reason)
	}
}

func startedGame(t *testing.T, srv *testServer) (*testClient, *testClient, message) {
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.name, ana.country, beto.name, beto.country = "Ana", "CU", "Beto", "ES"
	ana.send(message{T: "create", Mode: modeClassic, TimeMs: 600_000})
	beto.send(message{T: "join", Code: ana.mySeeks(1)[0].ID})
	a, b := ana.waitFor("state"), beto.waitFor("state")
	if turnIsFirst(t, a.Save) == a.First {
		return ana, beto, a
	}
	return beto, ana, b
}

func bestPlay(t *testing.T, state message) string {
	t.Helper()
	mirror, err := classic.LoadMirror(state.Save, true, quiet{})
	if err != nil {
		t.Fatal(err)
	}
	defer mirror.Close()
	g := mirror.Game()
	rack := g.OpponentRack()
	if state.First {
		rack = g.Rack()
	}
	text, err := engine.BestMoves(g.Board(), engineRack(rack), 30)
	if err != nil {
		t.Fatal(err)
	}
	var moves []struct{ Description string }
	json.Unmarshal([]byte(text), &moves)
	for _, m := range moves {
		if i := strings.Index(m.Description, " ("); i > 0 {
			return m.Description[:i]
		}
	}
	t.Skip("ese atril no permite colocar palabra")
	return ""
}

func engineRack(spaced string) string {
	var b strings.Builder
	for _, tile := range strings.Fields(spaced) {
		if len([]rune(tile)) > 1 {
			b.WriteString("[" + tile + "]")
		} else {
			b.WriteString(tile)
		}
	}
	return b.String()
}
