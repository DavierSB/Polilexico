package classic

func (m *Match) afterChange() {
	status := m.game.Status()
	running := !m.paused && !m.closed && !status.Over
	m.runClocks(running, status.HumanToMove)
	m.chargeTime()
	m.schedulePenalty(running, status.HumanToMove)
	m.scheduleTimeout(running && status.HumanToMove)
	if running && !status.HumanToMove && !m.thinking {
		m.startBot()
	}
	m.notifier.Notify()
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

func (m *Match) scheduleTimeout(humanRunning bool) {
	m.timeout.Cancel()
	if m.clocks != nil && humanRunning {
		m.timeout.Set(m.clocks.humanLeft(), m.onTimeout)
	}
}

func (m *Match) onTimeout() {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.paused || m.closed || !m.game.Status().HumanToMove {
		return
	}
	if m.clocks.humanLeft() <= 0 {
		m.chargeTime()
		m.game.LoseOnTime()
	}
	m.afterChange()
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
	m.botError = errorText(err)
	m.afterChange()
}

func errorText(err error) string {
	if err == nil {
		return ""
	}
	return err.Error()
}
