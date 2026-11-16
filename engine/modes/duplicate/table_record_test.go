package duplicate

import (
	"encoding/json"
	"testing"
	"time"

	"lexico/engine/internal/core"
)

func TestTableRecordFollowsEachPlayer(t *testing.T) {
	table := finishedTestTable(t)
	ana, beto, master := tableLog(t, table, "Ana"), tableLog(t, table, "Beto"), tableLog(t, table, "")
	if ana.FinalHits != 2 || ana.FinalHuman != ana.FinalMaster || len(ana.Turns) != 2 {
		t.Fatalf("Ana: %+v", ana)
	}
	if beto.FinalHuman != 0 || beto.Turns[0].HumanStatus != core.KindTimeout {
		t.Fatalf("Beto: %+v", beto.Turns[0])
	}
	if master.FinalHits != 2 || master.Turns[1].HumanPlay != master.Turns[1].MasterPlay {
		t.Fatalf("Máster: %+v", master)
	}
}

func TestTableRecordWaitsForTheEnd(t *testing.T) {
	table, _ := newTestTable(t, true)
	if _, err := table.Record("Ana"); err == nil {
		t.Fatal("dio el registro antes de terminar")
	}
}

func finishedTestTable(t *testing.T) *Table {
	table, clock := newTestTable(t, true)
	table.game.SetMaxRounds(2)
	for round := 0; round < 2; round++ {
		if _, err := table.Propose("Ana", masterInput(table.game)); err != nil {
			t.Fatal(err)
		}
		clock.Advance(time.Minute)
		table.Ready("Ana")
		table.Ready("Beto")
	}
	if !table.Finished() {
		t.Fatalf("no terminó: %s", table.phase)
	}
	return table
}

func tableLog(t *testing.T, table *Table, player string) gameLog {
	t.Helper()
	rec, err := table.Record(player)
	var log gameLog
	if err != nil || json.Unmarshal([]byte(rec.Log), &log) != nil || rec.GCG == "" {
		t.Fatalf("%v %q", err, rec.GCG)
	}
	return log
}
