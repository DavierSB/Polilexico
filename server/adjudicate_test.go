package main

import (
	"context"
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

func TestTheOneWhoAsksTakesTheBlame(t *testing.T) {
	srv := newTestServer(t)
	ahead, behind, state := startedGame(t, srv)
	play(t, ahead, state)
	agreeToAdjourn(behind, ahead)
	settleAll(srv)
	waitStats(t, srv, ahead.name, ahead.country, "win")
	item := ahead.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 1 && m.Adjourned[0].Result != "" }).Adjourned[0]
	if item.Result != "win" {
		t.Fatalf("resultado %q, quería win", item.Result)
	}
	ahead.send(message{T: "seen", Code: item.ID})
	ahead.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 0 })
}

func TestAPlayRightBeforeAskingDoesNotCount(t *testing.T) {
	srv := newTestServer(t)
	first, second, state := startedGame(t, srv)
	play(t, first, state)
	play(t, second, second.waitUntil(func(m message) bool { return m.T == "state" && turnIsFirst(t, m.Save) == m.First }))
	agreeToAdjourn(second, first)
	settleAll(srv)
	waitStats(t, srv, first.name, first.country, "win")
}

func TestAdjournmentWithoutALeadIsAnnulled(t *testing.T) {
	srv := newTestServer(t)
	a, b, _ := startedGame(t, srv)
	agreeToAdjourn(a, b)
	settleAll(srv)
	item := b.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 1 && m.Adjourned[0].Result != "" }).Adjourned[0]
	if item.Result != "none" {
		t.Fatalf("resultado %q, quería none", item.Result)
	}
}

func TestAwayRivalAcceptsAfterTheLimit(t *testing.T) {
	shorten(t, &askAwayLimit, 300*time.Millisecond)
	srv := newTestServer(t)
	a, b, _ := startedGame(t, srv)
	b.conn.Close(websocket.StatusNormalClosure, "")
	a.waitUntil(func(m message) bool { return m.T == "rival" && !m.Online })
	a.send(message{T: "adjourn"})
	if m := a.waitFor("rejected"); !strings.Contains(m.Reason, "un minuto") {
		t.Fatalf("aviso inesperado: %q", m.Reason)
	}
	a.waitUntil(func(m message) bool { return m.T == "closed" && m.Reason == adjournedReason })
	again := srv.connect(t, srv.logIn(t, b.name, b.country))
	again.waitUntil(func(m message) bool { return m.T == "closed" && m.Reason == adjournedReason })
	if saved := waitAdjourned(t, srv); saved.Responsible != idOf(t, srv, b) {
		t.Fatalf("responsable %q, quería al desconectado", saved.Responsible)
	}
}

func TestReturningRivalSeesThePendingProposal(t *testing.T) {
	srv := newTestServer(t)
	a, b, _ := startedGame(t, srv)
	b.conn.Close(websocket.StatusNormalClosure, "")
	a.waitUntil(func(m message) bool { return m.T == "rival" && !m.Online })
	a.send(message{T: "adjourn"})
	a.waitFor("rejected")
	again := srv.connect(t, srv.logIn(t, b.name, b.country))
	again.waitFor("adjournAsk")
	again.send(message{T: "adjournReply", Accept: true})
	a.waitUntil(func(m message) bool { return m.T == "closed" && m.Reason == adjournedReason })
}

func TestBothAwayAdjournsAgainstTheFirstToLeave(t *testing.T) {
	shorten(t, &vacantLimit, 300*time.Millisecond)
	srv := newTestServer(t)
	a, b, _ := startedGame(t, srv)
	a.conn.Close(websocket.StatusNormalClosure, "")
	b.waitUntil(func(m message) bool { return m.T == "rival" && !m.Online })
	b.conn.Close(websocket.StatusNormalClosure, "")
	if saved := waitAdjourned(t, srv); saved.Responsible != idOf(t, srv, a) {
		t.Fatalf("responsable %q, quería al primero en irse", saved.Responsible)
	}
}

func idOf(t *testing.T, srv *testServer, c *testClient) string {
	p, _ := srv.store.Identify(context.Background(), srv.logIn(t, c.name, c.country).AccessToken)
	return p.ID
}

func TestResumeStartsAtOnceWhenBothAreOnline(t *testing.T) {
	srv := newTestServer(t)
	a, b, state := startedGame(t, srv)
	agreeToAdjourn(a, b)
	item := b.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 1 }).Adjourned[0]
	b.send(message{T: "resume", Code: item.ID})
	if again := a.waitFor("state"); again.First != state.First {
		t.Fatalf("cambió de asiento al retomar: antes %v, ahora %v", state.First, again.First)
	}
	b.waitFor("state")
}

func TestUntimedResumesWithTheRivalAway(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeClassic, Untimed: true})
	beto.send(message{T: "join", Code: ana.mySeeks(1)[0].ID})
	beto.waitFor("state")
	agreeToAdjourn(ana, beto)
	beto.conn.Close(websocket.StatusNormalClosure, "")
	item := ana.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 1 && !m.Adjourned[0].Online }).Adjourned[0]
	if !item.Untimed {
		t.Fatalf("la aplazada no se marca sin tiempo: %+v", item)
	}
	ana.send(message{T: "resume", Code: item.ID})
	ana.waitFor("state")
	ana.waitUntil(func(m message) bool { return m.T == "rival" && !m.Online })
	again := srv.connect(t, srv.logIn(t, "Beto", "ES"))
	again.waitFor("state")
}

func TestTimedResumeNeedsTheRivalOnline(t *testing.T) {
	srv := newTestServer(t)
	a, b, _ := startedGame(t, srv)
	agreeToAdjourn(a, b)
	b.conn.Close(websocket.StatusNormalClosure, "")
	item := a.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 1 && !m.Adjourned[0].Online }).Adjourned[0]
	a.send(message{T: "resume", Code: item.ID})
	if m := a.waitFor("error"); !strings.Contains(m.Reason, "no está conectado") {
		t.Fatalf("error inesperado: %q", m.Reason)
	}
}

func agreeToAdjourn(asker, other *testClient) {
	asker.send(message{T: "adjourn"})
	other.waitFor("adjournAsk")
	other.send(message{T: "adjournReply", Accept: true})
	asker.waitUntil(func(m message) bool { return m.T == "closed" && m.Reason == adjournedReason })
}

func shorten(t *testing.T, limit *time.Duration, d time.Duration) {
	old := *limit
	*limit = d
	t.Cleanup(func() { *limit = old })
}

func waitAdjourned(t *testing.T, srv *testServer) adjourned {
	t.Helper()
	for range 50 {
		srv.hub.mu.Lock()
		for _, a := range srv.hub.adjourned {
			srv.hub.mu.Unlock()
			return a
		}
		srv.hub.mu.Unlock()
		time.Sleep(100 * time.Millisecond)
	}
	t.Fatal("no se aplazó la partida")
	return adjourned{}
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

func TestTwoPlayerDuplicateAdjournsAndResumes(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeDuplicate, TurnMs: 60_000})
	code := ana.mySeeks(1)[0].ID
	beto.send(message{T: "join", Code: code})
	beto.waitFor("room")
	ana.send(message{T: "start", Code: code})
	beto.waitFor("table")
	ana.waitFor("table")
	ana.send(message{T: "adjourn"})
	beto.waitFor("adjournAsk")
	beto.send(message{T: "adjournReply", Accept: true})
	ana.waitUntil(func(m message) bool { return m.T == "closed" && m.Reason == adjournedReason })
	item := beto.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 1 }).Adjourned[0]
	if !item.Duplicate {
		t.Fatalf("la aplazada no se marca como duplicada: %+v", item)
	}
	beto.send(message{T: "resume", Code: item.ID})
	ana.waitFor("table")
	beto.waitFor("table")
}

func TestDuplicateAdjournsWhenTheRivalStaysAway(t *testing.T) {
	shorten(t, &askAwayLimit, 300*time.Millisecond)
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeDuplicate, TurnMs: 60_000})
	code := ana.mySeeks(1)[0].ID
	beto.send(message{T: "join", Code: code})
	beto.waitFor("room")
	ana.send(message{T: "start", Code: code})
	beto.waitFor("table")
	beto.conn.Close(websocket.StatusNormalClosure, "")
	ana.waitUntil(func(m message) bool { return m.T == "rival" && !m.Online })
	adjournAlone(ana)
	settleAll(srv)
	item := ana.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Adjourned) == 1 && m.Adjourned[0].Result != "" }).Adjourned[0]
	if item.Result != "none" {
		t.Fatalf("a 0-0 debía anularse, salió %q", item.Result)
	}
}

func TestDuplicateAbandonedByEveryoneCloses(t *testing.T) {
	abandonAfter = 50 * time.Millisecond
	defer func() { abandonAfter = 5 * time.Minute }()
	srv := newTestServer(t)
	ana, beto, carla := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES"), srv.player(t, "Carla", "MX")
	ana.send(message{T: "create", Mode: modeDuplicate, TurnMs: 60_000})
	code := ana.mySeeks(1)[0].ID
	beto.send(message{T: "join", Code: code})
	carla.send(message{T: "join", Code: code})
	carla.waitFor("room")
	ana.send(message{T: "start", Code: code})
	carla.waitFor("table")
	for _, c := range []*testClient{ana, beto, carla} {
		c.conn.Close(websocket.StatusNormalClosure, "")
	}
	spy := srv.player(t, "Dani", "PE")
	spy.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Games) == 0 })
}
