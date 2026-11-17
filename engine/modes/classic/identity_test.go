package classic

import (
	"encoding/json"
	"os"
	"testing"

	"lexico/engine/events"
	"lexico/engine/internal/timing"
)

func TestIdentityTravelsInSaveAndRecord(t *testing.T) {
	m := finishedIdentifiedMatch(t)
	mirror := mustLoad(t, LoadMirror, m)
	log := recordOf(t, mirror)
	if log.HumanName != "Ana" || log.Countries[0] != "CU" || log.Countries[1] != "ES" || log.IDs[1] != "b-2" {
		t.Fatalf("%+v", log)
	}
}

func TestGuestRecordIsSeenFromTheGuest(t *testing.T) {
	m := finishedIdentifiedMatch(t)
	host := recordOf(t, mustLoad(t, LoadMirror, m))
	guest := recordOf(t, mustLoad(t, LoadGuestMirror, m))
	if guest.HumanName != "Beto" || guest.BotName != "Ana" || guest.IDs[0] != "b-2" || guest.Countries[1] != "CU" {
		t.Fatalf("%+v", guest)
	}
	if guest.FinalScores[0] != host.FinalScores[1] || guest.Winner != host.Winner {
		t.Fatalf("%v %v", guest.FinalScores, host.FinalScores)
	}
}

func TestRecordWithoutIdentityOmitsIt(t *testing.T) {
	m := startMatch(newLocalGame(t, StarterBot), nil, timing.NewFake(), newTestListener(), true)
	t.Cleanup(m.Close)
	m.game.LoseOnTime()
	data, err := os.ReadFile(m.Game().Result().RecordPath)
	if err != nil {
		t.Fatal(err)
	}
	var raw map[string]any
	if json.Unmarshal(data, &raw) != nil || raw["ids"] != nil || raw["countries"] != nil {
		t.Fatalf("%s", data)
	}
}

func finishedIdentifiedMatch(t *testing.T) *Match {
	t.Helper()
	m := startMatch(newLocalGame(t, StarterBot), nil, timing.NewFake(), newTestListener(), true)
	t.Cleanup(m.Close)
	m.SetIdentity(true, "CU", "a-1")
	m.SetIdentity(false, "ES", "b-2")
	m.game.LoseOnTime()
	m.game.recordPath = ""
	return m
}

func mustLoad(t *testing.T, load func(string, bool, events.Listener) (*Match, error), m *Match) *Match {
	t.Helper()
	text, err := m.Save()
	if err != nil {
		t.Fatal(err)
	}
	loaded, err := load(text, true, newTestListener())
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(loaded.Close)
	return loaded
}

func recordOf(t *testing.T, m *Match) gameLog {
	t.Helper()
	var log gameLog
	data, err := os.ReadFile(m.Game().Result().RecordPath)
	if err != nil || json.Unmarshal(data, &log) != nil {
		t.Fatalf("%v %s", err, data)
	}
	return log
}
