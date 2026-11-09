package main

import (
	"testing"
	"time"
)

func TestJoiningDuringAGameIsRefused(t *testing.T) {
	srv := newTestServer(t)
	ana, beto, caro := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES"), srv.player(t, "Caro", "MX")
	ana.send(message{T: "create", Mode: modeClassic})
	beto.send(message{T: "join", Code: ana.mySeeks(1)[0].ID})
	beto.waitFor("state")
	caro.send(message{T: "create", Mode: modeClassic})
	code := caro.mySeeks(1)[0].ID
	beto.send(message{T: "join", Code: code})
	beto.waitFor("error")
	if g := gamesOf(srv.hub, "Beto"); g != 1 {
		t.Fatalf("Beto en %d partidas", g)
	}
}

func TestPlayerInTwoGamesKeepsOnlyOne(t *testing.T) {
	srv := newTestServer(t)
	ana, beto, caro := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES"), srv.player(t, "Caro", "MX")
	ana.send(message{T: "create", Mode: modeClassic})
	beto.send(message{T: "join", Code: ana.mySeeks(1)[0].ID})
	beto.waitFor("state")
	caro.send(message{T: "create", Mode: modeClassic})
	code := caro.mySeeks(1)[0].ID
	beto.name, beto.country = "Beto", "ES"
	id := idOf(t, srv, beto)
	srv.hub.roomByCode(code).enter(srv.hub.clientOf(id))
	waitGames(t, srv.hub, "Beto", 1)
	if a := srv.hub.adjournedItems(id); len(a) != 1 {
		t.Fatalf("aplazadas: %d", len(a))
	}
}

func gamesOf(h *hub, name string) int {
	n := 0
	for _, g := range h.liveGames() {
		for _, p := range g.Players {
			if p == name {
				n++
			}
		}
	}
	return n
}

func waitGames(t *testing.T, h *hub, name string, want int) {
	t.Helper()
	deadline := time.Now().Add(5 * time.Second)
	for gamesOf(h, name) != want {
		if time.Now().After(deadline) {
			t.Fatalf("%s sigue en %d partidas", name, gamesOf(h, name))
		}
		time.Sleep(20 * time.Millisecond)
	}
}
