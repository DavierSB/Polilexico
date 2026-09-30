package classic

func (m *Match) afterChange() {
	status := m.game.Status()
	running := !m.paused && !m.closed && !status.Over
	m.runClocks(running, status.HumanToMove)
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
