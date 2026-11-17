package main

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"net/http"
	"testing"
)

func TestPhotoIsServedByUserID(t *testing.T) {
	srv := newTestServer(t)
	sess := srv.signUp(t, "Ana", "CU")
	p, err := srv.store.Identify(context.Background(), sess.AccessToken)
	if err != nil {
		t.Fatal(err)
	}
	jpeg := base64.StdEncoding.EncodeToString([]byte{0xFF, 0xD8, 0xFF, 0xE0, 1})
	srv.store.SavePhoto(context.Background(), p, jpeg)
	if res, _ := http.Get(srv.URL + "/photo?id=" + p.ID); res.StatusCode != http.StatusOK || res.Header.Get("ETag") == "" {
		t.Fatal(res.Status)
	}
	for _, bad := range []string{"desconocido", "a'b", "00ff"} {
		if res, _ := http.Get(srv.URL + "/photo?id=" + bad); res.StatusCode != http.StatusNotFound {
			t.Fatalf("%s: %s", bad, res.Status)
		}
	}
}

func TestClassicSaveCarriesBothIdentities(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeClassic})
	beto.send(message{T: "join", Code: ana.mySeeks(1)[0].ID})
	log := savedLog(t, beto.waitFor("state").Save)
	if len(log.Countries) != 2 || log.Countries[0] != "CU" || log.Countries[1] != "ES" || len(log.IDs) != 2 || log.IDs[1] == "" {
		t.Fatalf("%+v", log)
	}
}

type savedIdentity struct {
	Countries []string `json:"countries"`
	IDs       []string `json:"ids"`
}

func savedLog(t *testing.T, save string) savedIdentity {
	t.Helper()
	var sm struct{ Game string }
	var sg struct{ Log savedIdentity }
	if json.Unmarshal([]byte(save), &sm) != nil || json.Unmarshal([]byte(sm.Game), &sg) != nil {
		t.Fatal(save)
	}
	return sg.Log
}
