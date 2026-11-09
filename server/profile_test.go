package main

import (
	"net/http"
	"testing"
)

func TestProfileShowsAnotherPlayersStats(t *testing.T) {
	srv := newTestServer(t)
	srv.signUp(t, "Beto", "ES")
	sess := srv.signUp(t, "Ana", "CU")
	req, _ := http.NewRequest("GET", srv.URL+"/profile?name=beto&country=ES", nil)
	req.Header.Set("Authorization", "Bearer "+sess.AccessToken)
	res, err := http.DefaultClient.Do(req)
	if err != nil || res.StatusCode != 200 {
		t.Fatalf("%v %v", err, res.StatusCode)
	}
	req.URL.RawQuery = "name=nadie&country=ES"
	if res, _ := http.DefaultClient.Do(req); res.StatusCode != 400 {
		t.Fatalf("desconocido: %d", res.StatusCode)
	}
}
