package main

import (
	"testing"
	"time"

	"lexico/engine/modes/classic"
)

func TestSilenceAcceptsTheLastPlay(t *testing.T) {
	r := finalPassRoom(t, finalProtocol)
	if ms := r.finalMsFor(r.seats[0]); ms <= 0 || ms > (finalPassDelay+finalPassWindow).Milliseconds() {
		t.Fatalf("plazo %d ms", ms)
	}
	eventually(t, func() bool { return r.match.Game().Status().Over })
}

func TestAnOldClientGetsNoDeadline(t *testing.T) {
	r := finalPassRoom(t, finalProtocol-1)
	time.Sleep(finalPassDelay + 2*finalPassWindow)
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.match.Game().Status().Over || r.finalMsFor(r.seats[0]) != 0 {
		t.Fatal("aceptó sin popup")
	}
}

func TestAClaimCancelsTheDeadline(t *testing.T) {
	r := finalPassRoom(t, finalProtocol)
	r.mu.Lock()
	_, err := r.match.Play("reclamar")
	r.mu.Unlock()
	if err != nil || r.match.Game().LastClaim() == nil {
		t.Fatal(err)
	}
	eventually(t, func() bool {
		r.mu.Lock()
		defer r.mu.Unlock()
		return r.final.at.IsZero()
	})
}

func finalPassRoom(t *testing.T, version int) *classicRoom {
	t.Helper()
	shortenFinalPass(t)
	srv := newTestServer(t)
	srv.connectAs(t, srv.signUp(t, "Ana", "CU"), version).waitFor("lobby")
	ana := onlinePlayer(t, srv, "Ana")
	for range 60 {
		if r := botOutRoom(t, srv, ana); r != nil {
			return r
		}
	}
	t.Fatal("el bot nunca se quedó sin fichas")
	return nil
}

func botOutRoom(t *testing.T, srv *testServer, ana player) *classicRoom {
	t.Helper()
	r := &classicRoom{hub: srv.hub, seats: []*seat{{player: ana}, {player: player{ID: "bot"}}}}
	m, err := classic.NewMatch("BasicBot", classic.StarterHuman, 0, 0, true, listener(r.changed))
	if err != nil {
		t.Fatal(err)
	}
	m.SetClaims(true)
	r.mu.Lock()
	r.match = m
	r.mu.Unlock()
	t.Cleanup(m.Close)
	if !passUntilFinal(t, r) {
		return nil
	}
	return r
}

func passUntilFinal(t *testing.T, r *classicRoom) bool {
	t.Helper()
	eventually(t, func() bool {
		g := r.match.Game()
		if !g.FinalPass() && g.Status().HumanToMove && !g.Status().Over {
			r.match.Play("pasar")
		}
		return g.FinalPass() || g.Status().Over
	})
	return r.match.Game().FinalPass()
}

func shortenFinalPass(t *testing.T) {
	delay, window := finalPassDelay, finalPassWindow
	finalPassDelay, finalPassWindow = 20*time.Millisecond, 80*time.Millisecond
	t.Cleanup(func() { finalPassDelay, finalPassWindow = delay, window })
}

func onlinePlayer(t *testing.T, srv *testServer, name string) player {
	t.Helper()
	srv.hub.mu.Lock()
	defer srv.hub.mu.Unlock()
	for _, c := range srv.hub.online {
		if c.player.Name == name {
			return c.player
		}
	}
	t.Fatal("no está conectado: " + name)
	return player{}
}

func eventually(t *testing.T, done func() bool) {
	t.Helper()
	for end := time.Now().Add(90 * time.Second); time.Now().Before(end); time.Sleep(10 * time.Millisecond) {
		if done() {
			return
		}
	}
	t.Fatal("no llegó a tiempo")
}
