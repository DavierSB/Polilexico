package classic

import (
	"lexico/engine/events"
	"lexico/engine/internal/timing"
)

func (m *Match) afterChange() {
	status := m.game.Status()
	running := !m.paused && !m.closed && !status.Over
	ticking := running && m.personToMove(status) && !m.mirror
	m.runClocks(running, status.HumanToMove)
	m.chargeTime()
	m.schedulePenalty(running, status.HumanToMove)
	m.scheduleTimeout(ticking, status.HumanToMove)
	m.scheduleMarks(ticking, status.HumanToMove)
	m.noteEnding(status.Over)
	if running && !m.personToMove(status) && !m.thinking && !m.mirror {
		m.startBot()
	}
	m.notifier.Notify()
}

func (m *Match) personToMove(status *Status) bool {
	return status.HumanToMove || m.game.local
}

func (m *Match) runClocks(running, humanToMove bool) {
	if m.clocks == nil {
		return
	}
	if running {
		m.clocks.run(humanToMove)
	} else {
		m.clocks.stop()
	}
}

func (m *Match) chargeTime() {
	if m.clocks != nil {
		m.game.setTimePenalties(m.clocks.penalty(m.clocks.human), m.clocks.penalty(m.clocks.bot))
	}
}

func (m *Match) schedulePenalty(running, humanToMove bool) {
	m.penalty.Cancel()
	if m.clocks == nil || !running {
		return
	}
	if wait, ok := m.clocks.untilPenalty(m.clocks.side(humanToMove)); ok {
		m.penalty.Set(wait, m.onPenalty)
	}
}

func (m *Match) onPenalty() {
	m.mu.Lock()
	defer m.mu.Unlock()
	if !m.paused && !m.closed {
		m.afterChange()
	}
}

func (m *Match) scheduleTimeout(ticking, human bool) {
	m.timeout.Cancel()
	if m.clocks != nil && ticking {
		m.timeout.Set(m.clocks.left(human), m.onTimeout)
	}
}

func (m *Match) onTimeout() {
	m.mu.Lock()
	defer m.mu.Unlock()
	status := m.game.Status()
	if m.paused || m.closed || status.Over || !m.personToMove(status) {
		return
	}
	if m.clocks.left(status.HumanToMove) <= 0 {
		m.chargeTime()
		m.game.LoseOnTime()
		m.cues.Emit(events.CueTimeUp)
	}
	m.afterChange()
}

func (m *Match) scheduleMarks(ticking, human bool) {
	if m.clocks != nil {
		m.marks.Sync(ticking, m.clocks.left(human), m.onMark)
	}
}

func (m *Match) onMark(mark timing.Mark) {
	m.mu.Lock()
	defer m.mu.Unlock()
	status := m.game.Status()
	if m.paused || m.closed || status.Over || !m.personToMove(status) || m.clocks.left(status.HumanToMove) > mark.Left {
		return
	}
	m.cues.Emit(mark.Cue)
	m.afterChange()
}

func (m *Match) noteEnding(over bool) {
	if over && !m.over && m.game.humanWonMatch() {
		m.cues.Emit(events.CueVictory)
	}
	m.over = over
}

func (m *Match) startBot() {
	m.thinking = true
	go m.botTurn()
}

func (m *Match) botTurn() {
	choice, err := m.game.chooseBotMove()
	m.mu.Lock()
	defer m.mu.Unlock()
	m.thinking = false
	if m.closed || m.paused {
		return
	}
	if err == nil {
		m.chargeTime()
		_, err = m.game.applyBotMove(choice)
	}
	if err == nil {
		m.cues.Emit(events.CueOpponent)
	}
	m.botError = errorText(err)
	m.afterChange()
}

func errorText(err error) string {
	if err == nil {
		return ""
	}
	return err.Error()
}
