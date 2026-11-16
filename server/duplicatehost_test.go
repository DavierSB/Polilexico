package main

import (
	"testing"
	"time"

	"github.com/coder/websocket"
)

func TestDuplicateHostCanComeBack(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	code := waitingDuplicate(ana, beto)
	ana.conn.Close(websocket.StatusNormalClosure, "")
	beto.waitUntil(func(m message) bool { return m.T == "room" && m.HostAway })
	again := srv.connect(t, srv.logIn(t, "Ana", "CU"))
	again.waitFor("room")
	beto.waitUntil(func(m message) bool { return m.T == "room" && !m.HostAway && len(m.Players) == 2 })
	again.send(message{T: "start", Code: code})
	beto.waitFor("table")
}

func TestDuplicateClosesWhenTheHostDoesNotReturn(t *testing.T) {
	hostGrace = 50 * time.Millisecond
	defer func() { hostGrace = time.Minute }()
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	waitingDuplicate(ana, beto)
	ana.conn.Close(websocket.StatusNormalClosure, "")
	if m := beto.waitFor("closed"); m.Reason != hostClosed {
		t.Fatalf("motivo: %q", m.Reason)
	}
}

func TestDuplicateHostCancelSaysWhoClosed(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	waitingDuplicate(ana, beto)
	ana.send(message{T: "leave"})
	if m := beto.waitFor("closed"); m.Reason != hostClosed {
		t.Fatalf("motivo: %q", m.Reason)
	}
}

func waitingDuplicate(host, guest *testClient) string {
	host.send(message{T: "create", Mode: modeDuplicate, TurnMs: 60_000})
	code := host.mySeeks(1)[0].ID
	guest.send(message{T: "join", Code: code})
	guest.waitFor("room")
	return code
}
