package duplicate

import "time"

func (m *Match) afterChange() {
	running := !m.paused && !m.closed
	m.turn.Sync(running && usesTurnClock(m.phase), m.onTurnEnd)
	m.step.Sync(running && usesStepClock(m.phase), m.onStepEnd)
	m.notifier.Notify()
}

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

func (m *Match) enterPlaying(fresh bool) {
	m.phase = PhasePlaying
	m.proposal, m.input = nil, ""
	if fresh {
		m.turn.Restart(m.turnTime)
	}
}

func (m *Match) propose(attempt *Attempt, input string) {
	if attempt.Immediate {
		m.closeRound(m.game.Confirm(input))
		return
	}
	m.phase, m.proposal, m.input = PhaseConfirming, attempt, input
	m.step.Restart(CancelSeconds * time.Second)
}

func (m *Match) onTurnEnd() {
	m.whenDue(m.turn.Left, func() {
		if m.phase == PhaseConfirming {
			m.closeRound(m.game.Confirm(m.input))
		} else if m.phase == PhasePlaying {
			m.closeRound(m.game.TimeOut())
		}
	})
}

func (m *Match) onStepEnd() {
	m.whenDue(m.step.Left, func() {
		if m.phase == PhaseInvalidRack {
			m.enterPlaying(true)
		} else if m.phase == PhaseConfirming {
			m.closeRound(m.game.Confirm(m.input))
		}
	})
}

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
