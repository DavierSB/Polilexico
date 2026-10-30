package duplicate

import (
	"encoding/json"
	"testing"
	"time"

	"lexico/engine/internal/core"
	"lexico/engine/internal/timing"
)

func TestTableScoresEveryPlayerAgainstTheMaster(t *testing.T) {
	table, clock := newTestTable(t, true)
	if _, err := table.Propose("Ana", masterInput(table.game)); err != nil {
		t.Fatal(err)
	}
	clock.Advance(time.Minute)
	plays := table.rounds[0].Plays
	if table.phase != TableResults || plays[0].Name != masterName || plays[1].Points != plays[0].Points || plays[2].Kind != core.KindTimeout {
		t.Fatalf("%s %+v %+v %+v", table.phase, *plays[0], *plays[1], *plays[2])
	}
}

func TestTableWaitsUntilEveryoneIsReady(t *testing.T) {
	table, clock := newTestTable(t, true)
	clock.Advance(time.Minute)
	table.Ready("Ana")
	if table.phase != TableResults {
		t.Fatal("empezó sin Beto")
	}
	table.Ready("Beto")
	if table.phase != TablePlaying || table.viewFor("Ana").Round != 2 {
		t.Fatalf("%s", table.phase)
	}
}

func TestTableDoesNotWaitForAbsentPlayers(t *testing.T) {
	table, clock := newTestTable(t, true)
	clock.Advance(time.Minute)
	table.SetPresent("Beto", false)
	table.Ready("Ana")
	if table.phase != TablePlaying {
		t.Fatal("esperó a un ausente")
	}
}

func TestTableHidesOthersWithoutLiveScores(t *testing.T) {
	table, clock := newTestTable(t, false)
	clock.Advance(time.Minute)
	if plays := table.viewFor("Ana").Rounds[0].Plays; len(plays) != 2 || plays[1].Name != "Ana" {
		t.Fatalf("%+v", plays)
	}
}

func TestTableRejectsPlaysAfterTheRound(t *testing.T) {
	table, clock := newTestTable(t, true)
	clock.Advance(time.Minute)
	if _, err := table.Propose("Ana", "pasar"); err == nil {
		t.Fatal("aceptó una jugada fuera de tiempo")
	}
}

func TestTableViewIsJSON(t *testing.T) {
	table, _ := newTestTable(t, true)
	text, err := table.View("Ana")
	var v tableView
	if err != nil || json.Unmarshal([]byte(text), &v) != nil || v.Rack == "" || v.RemainingMs <= 0 || v.Waiting != 2 {
		t.Fatalf("%v %s", err, text)
	}
}

func newTestTable(t *testing.T, live bool) (*Table, *timing.Fake) {
	t.Helper()
	g := newTestGame(t)
	g.SetInvalidPlayLosesTurn(true)
	clock := timing.NewFake()
	table := startTable(g, []string{"Ana", "Beto"}, time.Minute, clock, live, testListener{})
	t.Cleanup(table.Close)
	return table, clock
}

type testListener struct{}

func (testListener) OnChange() {}
