package classic

import (
	"time"

	"lexico/engine/internal/timing"
)

const (
	RunningHuman = "human"
	RunningBot   = "bot"
)

type Clocks struct {
	HumanMs         int64
	HumanOvertimeMs int64
	BotMs           int64
	BotOvertimeMs   int64
	Running         string
}

func (m *Match) Clocks() *Clocks {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.clocks == nil {
		return nil
	}
	return m.clocks.snapshot()
}

type clockPair struct {
	human    *timing.Stopwatch
	bot      *timing.Stopwatch
	time     time.Duration
	overtime time.Duration
}

func newClockPair(clock timing.Clock, timeMs, overtimeMs, humanSpentMs, botSpentMs int64) *clockPair {
	if timeMs <= 0 {
		return nil
	}
	return &clockPair{human: timing.NewStopwatch(clock, ms(humanSpentMs)), bot: timing.NewStopwatch(clock, ms(botSpentMs)),
		time: ms(timeMs), overtime: ms(overtimeMs)}
}

func (p *clockPair) run(human bool) {
	if human {
		p.bot.Stop()
		p.human.Start()
	} else {
		p.human.Stop()
		p.bot.Start()
	}
}

func (p *clockPair) stop() {
	p.human.Stop()
	p.bot.Stop()
}

func (p *clockPair) humanLeft() time.Duration {
	return p.time + p.overtime - p.human.Spent()
}

func (p *clockPair) snapshot() *Clocks {
	c := &Clocks{Running: p.runningSide()}
	c.HumanMs, c.HumanOvertimeMs = p.remaining(p.human)
	c.BotMs, c.BotOvertimeMs = p.remaining(p.bot)
	return c
}

func (p *clockPair) remaining(w *timing.Stopwatch) (int64, int64) {
	left := p.time - w.Spent()
	return left.Milliseconds(), max(p.overtime+min(left, 0), 0).Milliseconds()
}

func (p *clockPair) runningSide() string {
	switch {
	case p.human.Running():
		return RunningHuman
	case p.bot.Running():
		return RunningBot
	}
	return ""
}

func ms(n int64) time.Duration {
	return time.Duration(n) * time.Millisecond
}
