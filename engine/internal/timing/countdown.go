package timing

import "time"

type Countdown struct {
	watch *Stopwatch
	alarm *Alarm
	limit time.Duration
}

func NewCountdown(clock Clock) *Countdown {
	return &Countdown{watch: NewStopwatch(clock, 0), alarm: NewAlarm(clock)}
}

func (c *Countdown) Restart(limit time.Duration) {
	c.watch.Reset()
	c.limit = limit
}

func (c *Countdown) Resume(limit, spent time.Duration) {
	c.watch = NewStopwatch(c.watch.clock, spent)
	c.limit = limit
}

func (c *Countdown) Sync(run bool, onEnd func()) {
	if !run {
		c.watch.Stop()
		c.alarm.Cancel()
		return
	}
	c.watch.Start()
	c.alarm.Set(c.Left(), onEnd)
}

func (c *Countdown) Left() time.Duration {
	return c.limit - c.watch.Spent()
}

func (c *Countdown) Spent() time.Duration {
	return c.watch.Spent()
}

func (c *Countdown) Limit() time.Duration {
	return c.limit
}
