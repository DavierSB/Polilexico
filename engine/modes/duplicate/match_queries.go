package duplicate

// Lo que la plataforma lee de la ronda en curso.

// Phase: la fase de la ronda (PhaseWaiting, PhasePlaying...).
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

// Rack: el atril de la ronda ("A CH E ?"), mientras se juega o se confirma; si no, "".
func (m *Match) Rack() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.draw == nil || !usesTurnClock(m.phase) {
		return ""
	}
	return m.draw.Rack
}

// InvalidRack: en PhaseInvalidRack, la mano que no valia; si no, "".
func (m *Match) InvalidRack() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.phase != PhaseInvalidRack {
		return ""
	}
	return m.draw.InitialRack
}

// Proposal: en PhaseConfirming, la jugada propuesta; si no, nil.
func (m *Match) Proposal() *Attempt {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.proposal
}

// TurnMs: el tiempo por turno de la partida.
func (m *Match) TurnMs() int64 {
	return m.turnTime.Milliseconds()
}

// TurnRemainingMs: lo que queda del turno mientras se juega o se confirma; si no, -1.
func (m *Match) TurnRemainingMs() int64 {
	m.mu.Lock()
	defer m.mu.Unlock()
	if !usesTurnClock(m.phase) {
		return -1
	}
	return max(m.turn.Left(), 0).Milliseconds()
}

// CancelRemainingMs: en PhaseConfirming, lo que queda para cancelar; si no, -1.
func (m *Match) CancelRemainingMs() int64 {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.phase != PhaseConfirming {
		return -1
	}
	return max(m.step.Left(), 0).Milliseconds()
}

// LastError: por que fallo la ultima ronda al anotarse ("" si no fallo).
func (m *Match) LastError() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.lastErr
}
