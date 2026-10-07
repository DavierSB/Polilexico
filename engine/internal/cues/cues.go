package cues

import (
	"time"

	"lexico/engine/events"
	"lexico/engine/internal/timing"
)

var Countdown = []timing.Mark{
	{Left: 30 * time.Second, Cue: events.CueWarning},
	{Left: 5 * time.Second, Cue: events.CueLastSeconds},
}

type Log struct {
	count int
	last  string
}

func (l *Log) Emit(cue string) {
	l.count++
	l.last = cue
}

func (l *Log) Count() int {
	return l.count
}

func (l *Log) Last() string {
	return l.last
}
