package duplicate

import (
	"testing"
	"time"
)

func TestTableSaveKeepsRoundsTotalsAndRack(t *testing.T) {
	table, clock := newTestTable(t, true)
	if _, err := table.Propose("Ana", masterInput(table.game)); err != nil {
		t.Fatal(err)
	}
	clock.Advance(time.Minute)
	table.Ready("Ana")
	table.Ready("Beto")
	rack := table.game.currentRack()
	text, err := table.Save()
	if err != nil {
		t.Fatal(err)
	}
	loaded, err := LoadTable(text, testListener{})
	if err != nil {
		t.Fatal(err)
	}
	defer loaded.Close()
	if len(loaded.rounds) != 1 || loaded.totals["Ana"] != table.totals["Ana"] || loaded.totals["Ana"] == 0 {
		t.Fatalf("rondas %d, total de Ana %d (antes %d)", len(loaded.rounds), loaded.totals["Ana"], table.totals["Ana"])
	}
	if loaded.phase != TablePlaying || loaded.game.currentRack() != rack {
		t.Fatalf("fase %s, atril %q (antes %q)", loaded.phase, loaded.game.currentRack(), rack)
	}
}
