package notify

import "lexico/engine/events"

type Notifier struct {
	pending chan struct{}
	done    chan struct{}
}

func New(l events.Listener) *Notifier {
	n := &Notifier{pending: make(chan struct{}, 1), done: make(chan struct{})}
	go n.deliver(l)
	return n
}

func (n *Notifier) Notify() {
	select {
	case n.pending <- struct{}{}:
	default:
	}
}

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
