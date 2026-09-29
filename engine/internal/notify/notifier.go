// Package notify entrega los avisos de una partida a su events.Listener desde un hilo propio.
package notify

import "lexico/engine/events"

// Notifier avisa al Listener en su propio hilo, de uno en uno. Varios avisos seguidos mientras
// el Listener esta ocupado se juntan en uno: todos dicen lo mismo, que el estado cambio.
type Notifier struct {
	pending chan struct{}
	done    chan struct{}
}

// New empieza a repartir avisos a l; con l nil, los avisos se descartan.
func New(l events.Listener) *Notifier {
	n := &Notifier{pending: make(chan struct{}, 1), done: make(chan struct{})}
	go n.deliver(l)
	return n
}

// Notify pide un aviso. No bloquea nunca: se puede llamar con la partida bloqueada.
func (n *Notifier) Notify() {
	select {
	case n.pending <- struct{}{}:
	default:
	}
}

// Close deja de avisar.
func (n *Notifier) Close() {
	close(n.done)
}

func (n *Notifier) deliver(l events.Listener) {
	for {
		select {
		case <-n.done:
			return
		case <-n.pending:
			if l != nil {
				l.OnChange()
			}
		}
	}
}
