package duplicate

import "time"

// El ritmo de la ronda: cada fase decide que plazos corren, y al agotarse uno la ronda avanza
// sola. Todo con m.mu tomado.

// afterChange pone los plazos al dia tras cualquier cambio y avisa a la plataforma.
func (m *Match) afterChange() {
	running := !m.paused && !m.closed
	m.turn.Sync(running && usesTurnClock(m.phase), m.onTurnEnd)
	m.step.Sync(running && usesStepClock(m.phase), m.onStepEnd)
	m.notifier.Notify()
}

// enterDraw: tras sacar el atril, la mano invalida (un rato), el turno o el final.
func (m *Match) enterDraw(draw *Draw) {
	m.draw = draw
	switch {
	case draw.GameOver:
		m.phase = PhaseFinished
	case draw.Redrawn:
		m.phase = PhaseInvalidRack
		m.step.Restart(InvalidRackSeconds * time.Second)
	default:
		m.enterPlaying(true)
	}
}

// enterPlaying: a pensar la jugada; `fresh` = turno nuevo, con el reloj entero.
func (m *Match) enterPlaying(fresh bool) {
	m.phase = PhasePlaying
	m.proposal, m.input = nil, ""
	if fresh {
		m.turn.Restart(m.turnTime)
	}
}

// propose: una colocacion espera la ventana para cancelar; lo demas se anota ya.
func (m *Match) propose(attempt *Attempt, input string) {
	if attempt.Immediate {
		m.closeRound(m.game.Confirm(input))
		return
	}
	m.phase, m.proposal, m.input = PhaseConfirming, attempt, input
	m.step.Restart(CancelSeconds * time.Second)
}

// onTurnEnd: se acabo el tiempo del turno. Sin jugada, 0 puntos; con una propuesta, se anota.
func (m *Match) onTurnEnd() {
	m.whenDue(m.turn.Left, func() {
		if m.phase == PhaseConfirming {
			m.closeRound(m.game.Confirm(m.input))
		} else if m.phase == PhasePlaying {
			m.closeRound(m.game.TimeOut())
		}
	})
}

// onStepEnd: termino la mano invalida (a jugar) o la ventana para cancelar (se anota).
func (m *Match) onStepEnd() {
	m.whenDue(m.step.Left, func() {
		if m.phase == PhaseInvalidRack {
			m.enterPlaying(true)
		} else if m.phase == PhaseConfirming {
			m.closeRound(m.game.Confirm(m.input))
		}
	})
}

// whenDue hace `advance` si la partida sigue en marcha y el plazo de verdad se agoto.
func (m *Match) whenDue(left func() time.Duration, advance func()) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.paused || m.closed {
		return
	}
	if left() <= 0 {
		advance()
	}
	m.afterChange()
}

// closeRound: la ronda quedo anotada (o fallo); a esperar la siguiente, o fin.
func (m *Match) closeRound(_ *Turn, err error) {
	m.lastErr = errorText(err)
	m.draw, m.proposal, m.input = nil, nil, ""
	m.phase = PhaseWaiting
	if m.game.Status().Over {
		m.phase = PhaseFinished
	}
}

func usesTurnClock(phase string) bool {
	return phase == PhasePlaying || phase == PhaseConfirming
}

func usesStepClock(phase string) bool {
	return phase == PhaseInvalidRack || phase == PhaseConfirming
}

func errorText(err error) string {
	if err == nil {
		return ""
	}
	return err.Error()
}
