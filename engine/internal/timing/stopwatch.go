package timing

import "time"

// Stopwatch acumula el tiempo que pasa mientras esta en marcha.
type Stopwatch struct {
	clock   Clock
	spent   time.Duration
	since   time.Time
	running bool
}

// NewStopwatch empieza parado, con `spent` ya gastado.
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

// Spent: lo gastado, contando el tramo en marcha.
func (s *Stopwatch) Spent() time.Duration {
	if !s.running {
		return s.spent
	}
	return s.spent + s.clock.Now().Sub(s.since)
}

func (s *Stopwatch) Running() bool {
	return s.running
}

// Reset lo deja parado y a cero.
func (s *Stopwatch) Reset() {
	s.spent = 0
	s.running = false
}
