package classic

import (
	"time"

	"lexico/engine/internal/timing"
)

// Quien tiene el reloj en marcha, en Clocks.Running.
const (
	RunningHuman = "human"
	RunningBot   = "bot"
)

// Clocks son los relojes en este momento, en milisegundos. HumanMs o BotMs negativo = se acabo
// el tiempo principal y corre el descuento, del que quedan HumanOvertimeMs o BotOvertimeMs.
// Running dice cual corre ("" = ninguno: pausa, partida terminada o esperando).
type Clocks struct {
	HumanMs         int64
	HumanOvertimeMs int64
	BotMs           int64
	BotOvertimeMs   int64
	Running         string
}

// Clocks: los relojes ahora mismo; nil si la partida es sin tiempo.
func (m *Match) Clocks() *Clocks {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.clocks == nil {
		return nil
	}
	return m.clocks.snapshot()
}

// clockPair son los dos cronometros de la partida, con el tiempo y el descuento de cada uno.
type clockPair struct {
	human    *timing.Stopwatch
	bot      *timing.Stopwatch
	time     time.Duration
	overtime time.Duration
}

// newClockPair: nil si timeMs no es positivo (partida sin tiempo).
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

// humanLeft: lo que te queda contando el descuento.
func (p *clockPair) humanLeft() time.Duration {
	return p.time + p.overtime - p.human.Spent()
}

func (p *clockPair) snapshot() *Clocks {
	c := &Clocks{Running: p.runningSide()}
	c.HumanMs, c.HumanOvertimeMs = p.remaining(p.human)
	c.BotMs, c.BotOvertimeMs = p.remaining(p.bot)
	return c
}

// remaining: el tiempo principal que queda (negativo en el descuento) y el descuento que queda.
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
