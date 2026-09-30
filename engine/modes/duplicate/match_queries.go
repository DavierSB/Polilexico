package duplicate

func (m *Match) Phase() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.phase
}

func (m *Match) Paused() bool {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.paused
}

func (m *Match) Rack() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.draw == nil || !usesTurnClock(m.phase) {
		return ""
	}
	return m.draw.Rack
}

func (m *Match) InvalidRack() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.phase != PhaseInvalidRack {
		return ""
	}
	return m.draw.InitialRack
}

func (m *Match) Proposal() *Attempt {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.proposal
}

func (m *Match) TurnMs() int64 {
	return m.turnTime.Milliseconds()
}

func (m *Match) TurnRemainingMs() int64 {
	m.mu.Lock()
	defer m.mu.Unlock()
	if !usesTurnClock(m.phase) {
		return -1
	}
	return max(m.turn.Left(), 0).Milliseconds()
}

func (m *Match) CancelRemainingMs() int64 {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.phase != PhaseConfirming {
		return -1
	}
	return max(m.step.Left(), 0).Milliseconds()
}

func (m *Match) LastError() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.lastErr
}
