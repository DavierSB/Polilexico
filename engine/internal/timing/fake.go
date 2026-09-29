package timing

import (
	"sort"
	"sync"
	"sync/atomic"
	"time"
)

// Fake es un reloj para las pruebas: la hora solo avanza con Advance, que dispara en el mismo
// hilo, y en orden, los temporizadores que vencen.
type Fake struct {
	mu     sync.Mutex
	now    time.Time
	timers []*fakeTimer
}

func NewFake() *Fake {
	return &Fake{now: time.Unix(0, 0)}
}

func (c *Fake) Now() time.Time {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.now
}

func (c *Fake) AfterFunc(d time.Duration, f func()) Timer {
	c.mu.Lock()
	defer c.mu.Unlock()
	t := &fakeTimer{at: c.now.Add(d), f: f}
	c.timers = append(c.timers, t)
	return t
}

// Advance adelanta la hora d y dispara los temporizadores vencidos (tambien los que estos
// programen dentro del plazo).
func (c *Fake) Advance(d time.Duration) {
	end := c.Now().Add(d)
	for t := c.nextDue(end); t != nil; t = c.nextDue(end) {
		t.f()
	}
	c.mu.Lock()
	c.now = end
	c.mu.Unlock()
}

// nextDue saca el temporizador vivo que antes vence hasta `end`, y pone la hora en su momento.
func (c *Fake) nextDue(end time.Time) *fakeTimer {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.dropStopped()
	sort.Slice(c.timers, func(i, j int) bool { return c.timers[i].at.Before(c.timers[j].at) })
	if len(c.timers) == 0 || c.timers[0].at.After(end) {
		return nil
	}
	t := c.timers[0]
	c.timers = c.timers[1:]
	c.now = t.at
	return t
}

func (c *Fake) dropStopped() {
	live := c.timers[:0]
	for _, t := range c.timers {
		if !t.stopped.Load() {
			live = append(live, t)
		}
	}
	c.timers = live
}

type fakeTimer struct {
	at      time.Time
	f       func()
	stopped atomic.Bool
}

func (t *fakeTimer) Stop() bool {
	return !t.stopped.Swap(true)
}
