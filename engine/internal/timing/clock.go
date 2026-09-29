// Package timing da la hora y los temporizadores de las partidas en marcha, con un reloj de
// verdad o uno falso para las pruebas.
package timing

import "time"

// Clock es la fuente de la hora y de los temporizadores.
type Clock interface {
	Now() time.Time
	AfterFunc(d time.Duration, f func()) Timer
}

// Timer es un temporizador ya programado.
type Timer interface {
	Stop() bool
}

// Real es el reloj del sistema.
func Real() Clock {
	return realClock{}
}

type realClock struct{}

func (realClock) Now() time.Time {
	return time.Now()
}

func (realClock) AfterFunc(d time.Duration, f func()) Timer {
	return time.AfterFunc(d, f)
}
