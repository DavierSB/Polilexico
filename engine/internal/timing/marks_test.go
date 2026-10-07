package timing

import (
	"testing"
	"time"
)

func TestMarksFireTheNextMarkAhead(t *testing.T) {
	clock := NewFake()
	marks := NewMarks(clock, []Mark{{30 * time.Second, "aviso"}, {5 * time.Second, "final"}})
	var got []string
	marks.Sync(true, 20*time.Second, func(m Mark) { got = append(got, m.Cue) })
	clock.Advance(15 * time.Second)
	if len(got) != 1 || got[0] != "final" {
		t.Fatalf("avisos %v", got)
	}
}

func TestMarksStayQuietWhileStopped(t *testing.T) {
	clock := NewFake()
	marks := NewMarks(clock, []Mark{{30 * time.Second, "aviso"}})
	fired := false
	marks.Sync(true, 40*time.Second, func(Mark) { fired = true })
	marks.Sync(false, 40*time.Second, func(Mark) { fired = true })
	clock.Advance(time.Minute)
	if fired {
		t.Fatal("avisó estando parado")
	}
}
