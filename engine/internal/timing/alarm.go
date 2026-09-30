package timing

import "time"

type Alarm struct {
	clock Clock
	timer Timer
}

func NewAlarm(clock Clock) *Alarm {
	return &Alarm{clock: clock}
}

func (a *Alarm) Set(d time.Duration, f func()) {
	a.Cancel()
	a.timer = a.clock.AfterFunc(max(d, 0), f)
}

func (a *Alarm) Cancel() {
	if a.timer != nil {
		a.timer.Stop()
		a.timer = nil
	}
}
