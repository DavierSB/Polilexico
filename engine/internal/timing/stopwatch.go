package timing

import "time"

type Stopwatch struct {
	clock   Clock
	spent   time.Duration
	since   time.Time
	running bool
}

func NewStopwatch(clock Clock, spent time.Duration) *Stopwatch {
	return &Stopwatch{clock: clock, spent: spent}
}

func (s *Stopwatch) Start() {
	if s.running {
		return
	}
	s.since = s.clock.Now()
	s.running = true
}

func (s *Stopwatch) Stop() {
	if !s.running {
		return
	}
	s.spent += s.clock.Now().Sub(s.since)
	s.running = false
}

func (s *Stopwatch) Spent() time.Duration {
	if !s.running {
		return s.spent
	}
	return s.spent + s.clock.Now().Sub(s.since)
}

func (s *Stopwatch) Running() bool {
	return s.running
}

func (s *Stopwatch) Reset() {
	s.spent = 0
	s.running = false
}
