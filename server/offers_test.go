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
