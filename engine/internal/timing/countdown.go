package timing

import "time"

// Countdown es una cuenta atras que se puede parar y seguir: cuando se agota estando en marcha,
// llama a la funcion que se le dio en Sync.
type Countdown struct {
	watch *Stopwatch
	alarm *Alarm
	limit time.Duration
}

func NewCountdown(clock Clock) *Countdown {
	return &Countdown{watch: NewStopwatch(clock, 0), alarm: NewAlarm(clock)}
}

// Restart la deja parada con `limit` por delante.
func (c *Countdown) Restart(limit time.Duration) {
	c.watch.Reset()
	c.limit = limit
}

// Resume la deja parada con `limit` en total, del que ya se gasto `spent`.
func (c *Countdown) Resume(limit, spent time.Duration) {
	c.watch = NewStopwatch(c.watch.clock, spent)
	c.limit = limit
}

// Sync la pone en marcha (y avisara con onEnd al agotarse) o la para.
func (c *Countdown) Sync(run bool, onEnd func()) {
	if !run {
		c.watch.Stop()
		c.alarm.Cancel()
		return
	}
	c.watch.Start()
	c.alarm.Set(c.Left(), onEnd)
}

// Left: lo que queda (negativo si ya se paso).
func (c *Countdown) Left() time.Duration {
	return c.limit - c.watch.Spent()
}

// Spent: lo que ya se gasto.
func (c *Countdown) Spent() time.Duration {
	return c.watch.Spent()
}

func (c *Countdown) Limit() time.Duration {
	return c.limit
}
