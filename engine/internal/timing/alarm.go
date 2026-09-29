package timing

import "time"

// Alarm llama a una funcion cuando pasa un tiempo; programarla de nuevo cancela la anterior.
type Alarm struct {
	clock Clock
	timer Timer
}

func NewAlarm(clock Clock) *Alarm {
	return &Alarm{clock: clock}
}

// Set programa f para dentro de d (ya, si d no es positivo).
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
