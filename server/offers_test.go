package main

import "testing"

func TestNewDuplicateIsAnnouncedToIdlePlayers(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeDuplicate})
	offer := beto.waitFor("duplicateOffer")
	if offer.Name != "Ana" || offer.Code == "" {
		t.Fatalf("%+v", offer)
	}
}

func TestLiveDuplicateShowsItsRound(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeDuplicate, TurnMs: 30_000})
	code := ana.mySeeks(1)[0].ID
	beto.send(message{T: "join", Code: code})
	ana.send(message{T: "start", Code: code})
	beto.waitFor("table")
	games := srv.hub.liveGames()
	if len(games) != 1 || games[0].Round != 1 {
		t.Fatalf("%+v", games)
	}
}
