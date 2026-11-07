package main

import "testing"

func TestOldClientsStillConnect(t *testing.T) {
	srv := newTestServer(t)
	srv.connectAs(t, srv.signUp(t, "Ana", "CU"), minProtocol)
}

func TestTellReachesAnyoneOnline(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "tell", Name: "beto", Country: "ES", Text: "hola"})
	m := beto.waitFor("tell")
	if m.Name != "Ana" || m.Country != "CU" || m.Text != "hola" {
		t.Fatalf("mensaje %+v", m)
	}
}

func TestTellToAnOldClientOutsideTheGameIsRefused(t *testing.T) {
	srv := newTestServer(t)
	ana := srv.player(t, "Ana", "CU")
	srv.connectAs(t, srv.signUp(t, "Beto", "ES"), minProtocol)
	ana.send(message{T: "tell", Name: "Beto", Country: "ES", Text: "hola"})
	if m := ana.waitFor("error"); m.Reason != "Beto tiene una versión anterior y no puede recibir mensajes." {
		t.Fatalf("aviso %q", m.Reason)
	}
}
