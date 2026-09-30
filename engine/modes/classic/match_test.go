package classic

import (
	"testing"
	"time"

	"lexico/engine/internal/testenv"
	"lexico/engine/internal/timing"
)

func TestMatchBotPlaysAfterYou(t *testing.T) {
	m, _, l := newTestMatch(t, 0, false)
	playYourTurn(t, m)
	l.waitFor(t, func() bool { return m.Game().Status().HumanToMove })
	if m.Game().MoveCount() != 2 {
		t.Fatalf("jugadas: %d", m.Game().MoveCount())
	}
}

func TestMatchBotWaitsForResume(t *testing.T) {
	g := newTestGame(t)
	for g.HumanStarts() {
		g = newTestGame(t)
	}
	l := newTestListener()
	m := startMatch(g, nil, timing.NewFake(), l, true)
	t.Cleanup(m.Close)
	time.Sleep(100 * time.Millisecond)
	if m.Game().MoveCount() != 0 {
		t.Fatal("el bot jugó en pausa")
	}
	m.Resume()
	l.waitFor(t, func() bool { return m.Game().MoveCount() == 1 })
}

func TestMatchClocksRunOnlyWhilePlaying(t *testing.T) {
	m, clock, _ := newTestMatch(t, 60_000, false)
	clock.Advance(10 * time.Second)
	m.Pause()
	clock.Advance(time.Minute)
	if c := m.Clocks(); c.HumanMs != 50_000 || c.Running != "" {
		t.Fatalf("%+v", *c)
	}
}

func TestMatchLosesOnTime(t *testing.T) {
	m, clock, _ := newTestMatch(t, 60_000, false)
	clock.Advance(80 * time.Second)
	if c := m.Clocks(); c.HumanMs != -20_000 || c.HumanOvertimeMs != 10_000 {
		t.Fatalf("en el descuento: %+v", *c)
	}
	clock.Advance(10 * time.Second)
	if r := m.Game().Result(); r == nil || !r.LostOnTime {
		t.Fatalf("no perdió por tiempo: %+v", r)
	}
}

func TestMatchSaveKeepsTimeAndLoadsPaused(t *testing.T) {
	m, clock, _ := newTestMatch(t, 60_000, true)
	clock.Advance(15 * time.Second)
	text, err := m.Save()
	if err != nil {
		t.Fatal(err)
	}
	loaded, err := LoadMatch(text, nil)
	if err != nil {
		t.Fatal(err)
	}
	if c := loaded.Clocks(); !loaded.Paused() || c.HumanMs != 45_000 || !loaded.Game().invalidLosesTurn {
		t.Fatalf("pausa %v, relojes %+v", loaded.Paused(), *c)
	}
}

func newTestMatch(t *testing.T, timeMs int64, invalidLosesTurn bool) (*Match, *timing.Fake, *testListener) {
	t.Helper()
	g := humanStartsWith(t, "TPNAEIO")
	g.SetInvalidPlayLosesTurn(invalidLosesTurn)
	clock := timing.NewFake()
	l := newTestListener()
	m := startMatch(g, newClockPair(clock, timeMs, 30_000, 0, 0), clock, l, false)
	t.Cleanup(m.Close)
	return m, clock, l
}

func playYourTurn(t *testing.T, m *Match) {
	t.Helper()
	if _, err := m.Play(testenv.Input(m.game.bot.GenerateMoves(1)[0])); err != nil {
		t.Fatal(err)
	}
}

type testListener struct {
	changed chan struct{}
}

func newTestListener() *testListener {
	return &testListener{changed: make(chan struct{}, 100)}
}

func (l *testListener) OnChange() {
	l.changed <- struct{}{}
}

func (l *testListener) waitFor(t *testing.T, cond func() bool) {
	t.Helper()
	deadline := time.After(10 * time.Second)
	for !cond() {
		select {
		case <-l.changed:
		case <-deadline:
			t.Fatal("no llegó el aviso esperado")
		}
	}
}

func TestMatchChargesEachOvertimeMinute(t *testing.T) {
	g := humanStartsWith(t, "TPNAEIO")
	clock := timing.NewFake()
	m := startMatch(g, newClockPair(clock, 60_000, 180_000, 0, 0), clock, newTestListener(), false)
	t.Cleanup(m.Close)
	steps := []struct {
		advance time.Duration
		penalty int
	}{
		{time.Minute, 0}, {time.Millisecond, 10}, {58 * time.Second, 10}, {time.Second, 20},
		{2 * time.Minute, 30}, {time.Second, 30},
	}
	for i, s := range steps {
		clock.Advance(s.advance)
		if got := -m.Game().Status().HumanScore; got != s.penalty {
			t.Fatalf("paso %d: descuento %d, esperaba %d", i, got, s.penalty)
		}
	}
	if r := m.Game().Result(); r == nil || !r.LostOnTime || r.HumanTime != 30 || r.HumanScore != -30 {
		t.Fatalf("%+v", r)
	}
}
