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

func TestAbsentPlayerLosesWhenTheOtherLeads(t *testing.T) {
	srv := newTestServer(t)
	onTurn, waiting, state := startedGame(t, srv)
	play(t, onTurn, state)
	waiting.conn.Close(websocket.StatusNormalClosure, "")
	onTurn.waitUntil(func(m message) bool { return m.T == "rival" && !m.Online })
	adjournAlone(onTurn)
	settleAll(srv)
	waitStats(t, srv, onTurn.name, onTurn.country, "win")
	item := onTurn.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 1 && m.Adjourned[0].Result != "" }).Adjourned[0]
	if item.Result != "win" {
		t.Fatalf("resultado %q, quería win", item.Result)
	}
	onTurn.send(message{T: "seen", Code: item.ID})
	onTurn.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 0 })
}

func TestAPlayRightBeforeLeavingDoesNotCount(t *testing.T) {
	srv := newTestServer(t)
	first, second, state := startedGame(t, srv)
	play(t, first, state)
	play(t, second, second.waitFor("state"))
	second.conn.Close(websocket.StatusNormalClosure, "")
	first.waitUntil(func(m message) bool { return m.T == "rival" && !m.Online })
	adjournAlone(first)
	settleAll(srv)
	waitStats(t, srv, first.name, first.country, "win")
}

func TestRefusingToResumeTakesTheBlame(t *testing.T) {
	srv := newTestServer(t)
	onTurn, waiting, state := startedGame(t, srv)
	play(t, onTurn, state)
	onTurn.send(message{T: "adjourn"})
	waiting.waitFor("adjournAsk")
	waiting.send(message{T: "adjournReply", Accept: true})
	item := onTurn.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 1 }).Adjourned[0]
	onTurn.send(message{T: "resume", Code: item.ID})
	invite := waiting.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Invites) == 1 }).Invites[0]
	waiting.send(message{T: "decline", Code: invite.ID})
	onTurn.waitUntil(func(m message) bool { return m.T == "closed" && strings.Contains(m.Reason, "no quiere retomar") })
	settleAll(srv)
	waitStats(t, srv, onTurn.name, onTurn.country, "win")
}

func TestAgreedAdjournmentIsAnnulledAtExpiry(t *testing.T) {
	srv := newTestServer(t)
	a, b, _ := startedGame(t, srv)
	a.send(message{T: "adjourn"})
	b.waitFor("adjournAsk")
	b.send(message{T: "adjournReply", Accept: true})
	a.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 1 })
	settleAll(srv)
	item := b.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 1 && m.Adjourned[0].Result != "" }).Adjourned[0]
	if item.Result != "none" {
		t.Fatalf("resultado %q, quería none", item.Result)
	}
}

func adjournAlone(c *testClient) {
	c.send(message{T: "adjourn"})
	c.waitUntil(func(m message) bool { return m.T == "closed" && m.Reason == adjournedReason })
}

func settleAll(srv *testServer) {
	srv.hub.sweep(time.Now().Add(adjournedTTL + time.Hour))
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
