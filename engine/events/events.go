// Package events es como el motor avisa a la plataforma de que algo cambio en una partida.
package events

// Listener recibe los avisos de una partida en marcha. OnChange llega cada vez que cambia algo
// (una jugada, un reloj que vence, una fase nueva, el final): la plataforma vuelve a leer el
// estado que le interese.
//
// Los avisos llegan en orden, de uno en uno, desde un hilo del motor y nunca con la partida
// bloqueada, asi que desde OnChange se puede leer el estado. Lo que no conviene es hacer ahi
// trabajo pesado: los avisos siguientes esperan a que OnChange termine.
type Listener interface {
	OnChange()
}
