package main

import (
	"encoding/json"
	"strings"
	"testing"
)

func TestNewClientsPlayWithClaims(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	if state := classicState(t, ana, beto); !strings.Contains(state, `Claims\":true`) {
		t.Fatal(state)
	}
}

func TestAnOldClientKeepsAutomaticChecks(t *testing.T) {
	srv := newTestServer(t)
	ana := srv.player(t, "Ana", "CU")
	beto := srv.connectAs(t, srv.signUp(t, "Beto", "ES"), claimProtocol-1)
	if state := classicState(t, ana, beto); strings.Contains(state, `Claims\":true`) {
		t.Fatal(state)
	}
}

func TestVoidRoomsHaveNoClaims(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeClassic, Void: true})
	beto.send(message{T: "join", Code: ana.mySeeks(1)[0].ID})
	if state := beto.waitFor("state").Save; strings.Contains(state, `Claims\":true`) {
		t.Fatal(state)
	}
}

func TestAnOldClientReturningTurnsClaimsOff(t *testing.T) {
	srv := newTestServer(t)
	ana := srv.player(t, "Ana", "CU")
	sess := srv.signUp(t, "Beto", "ES")
	classicState(t, ana, srv.connect(t, sess))
	old := srv.connectAs(t, sess, claimProtocol-1)
	old.waitUntil(func(m message) bool { return m.T == "state" && !strings.Contains(m.Save, `Claims\":true`) })
}

func classicState(t *testing.T, host, guest *testClient) string {
	t.Helper()
	host.send(message{T: "create", Mode: modeClassic, TimeMs: 600_000})
	guest.send(message{T: "join", Code: host.mySeeks(1)[0].ID})
	host.waitFor("state")
	return guest.waitUntil(func(m message) bool { return m.T == "state" }).Save
}

func TestTheRivalClaimsAPlay(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	first := classicState(t, ana, beto)
	onTurn, rival := beto, ana
	if turnIsFirst(t, first) {
		onTurn, rival = ana, beto
	}
	onTurn.send(message{T: "move", Input: "h8 " + rackOf(t, onTurn)[:3]})
	rival.waitUntil(func(m message) bool { return m.T == "state" && strings.Contains(m.Save, `Kind\":\"play`) })
	onTurn.send(message{T: "move", Input: "reclamar"})
	onTurn.waitFor("rejected")
	rival.send(message{T: "move", Input: "reclamar"})
	onTurn.waitUntil(func(m message) bool { return m.T == "state" && strings.Contains(m.Save, `LastClaim\":{\"Seq\":1`) })
}

func rackOf(t *testing.T, c *testClient) string {
	t.Helper()
	save := c.waitUntil(func(m message) bool { return m.T == "state" }).Save
	var sm struct{ Game string }
	var sg struct {
		History struct {
			Racks []string `json:"lastKnownRacks"`
		}
	}
	json.Unmarshal([]byte(save), &sm)
	json.Unmarshal([]byte(sm.Game), &sg)
	for _, r := range sg.History.Racks {
		if plain := plainLetters(r); len(plain) >= 3 {
			return plain
		}
	}
	t.Fatal(save)
	return ""
}

func plainLetters(rack string) string {
	var out []rune
	inside := false
	for _, r := range rack {
		switch {
		case r == '[' || r == ']':
			inside = r == '['
		case !inside && r >= 'A' && r <= 'Z':
			out = append(out, r)
		}
	}
	return string(out)
}
