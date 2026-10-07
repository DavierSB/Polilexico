package duplicate

import (
	"time"

	"lexico/engine/events"
	"lexico/engine/internal/timing"
)

func (m *Match) afterChange() {
	running := !m.paused && !m.closed
	m.turn.Sync(running && usesTurnClock(m.phase), m.onTurnEnd)
	m.marks.Sync(running && usesTurnClock(m.phase), m.turn.Left(), m.onMark)
	m.step.Sync(running && usesStepClock(m.phase), m.onStepEnd)
	m.notifier.Notify()
}

func (m *Match) enterDraw(draw *Draw) {
	m.draw = draw
	switch {
	case draw.GameOver:
		m.finish()
	case draw.ManyInvalid():
		m.phase = PhaseManyInvalid
		m.step.Restart(InvalidRackSeconds * time.Second)
	case draw.Redrawn:
		m.phase, m.shown = PhaseInvalidRack, 0
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
		if !usesTurnClock(m.phase) {
			return
		}
		if m.phase == PhaseConfirming {
			m.closeRound(m.game.Confirm(m.input))
		} else {
			m.closeRound(m.game.TimeOut())
		}
	})
}

func (m *Match) onMark(mark timing.Mark) {
	m.whenDue(func() time.Duration { return m.turn.Left() - mark.Left }, func() {
		if usesTurnClock(m.phase) {
			m.cues.Emit(mark.Cue)
		}
	})
}

func (m *Match) onStepEnd() {
	m.whenDue(m.step.Left, func() {
		switch m.phase {
		case PhaseInvalidRack:
			m.nextInvalidRack()
		case PhaseManyInvalid:
			m.enterPlaying(true)
		case PhaseConfirming:
			m.closeRound(m.game.Confirm(m.input))
		}
	})
}

func (m *Match) nextInvalidRack() {
	m.shown++
	if m.shown < len(m.draw.invalidRacks) {
		m.step.Restart(InvalidRackSeconds * time.Second)
		return
	}
	m.enterPlaying(true)
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

func (m *Match) closeRound(turn *Turn, err error) {
	m.lastErr = errorText(err)
	m.cueRound(turn)
	m.draw, m.proposal, m.input = nil, nil, ""
	m.phase = PhaseWaiting
	if m.game.Status().Over {
		m.finish()
	}
}

func (m *Match) cueRound(turn *Turn) {
	switch {
	case turn == nil:
	case turn.Hit:
		m.cues.Emit(events.CueCorrect)
	default:
		m.cues.Emit(events.CueMiss)
	}
}

func (m *Match) finish() {
	m.phase = PhaseFinished
	m.cues.Emit(events.CueGameOver)
}

func usesTurnClock(phase string) bool {
	return phase == PhasePlaying || phase == PhaseConfirming
}

func showsRack(phase string) bool {
	return usesTurnClock(phase) || phase == PhaseManyInvalid
}

func usesStepClock(phase string) bool {
	return phase == PhaseInvalidRack || phase == PhaseManyInvalid || phase == PhaseConfirming
}

func errorText(err error) string {
	if err == nil {
		return ""
	}
	return err.Error()
}
