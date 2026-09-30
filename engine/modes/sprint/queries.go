package sprint

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

func (m *Match) MaxLives() int {
	return m.maxLives
}

func (m *Match) Lives() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.lives
}

func (m *Match) Solved() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.solved
}

func (m *Match) Posed() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.posed
}

func (m *Match) TotalMs() int64 {
	return m.totalTime.Milliseconds()
}

func (m *Match) RemainingMs() int64 {
	m.mu.Lock()
	defer m.mu.Unlock()
	return max(m.clock.Left(), 0).Milliseconds()
}

func (m *Match) Board() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.puzzle == nil {
		return ""
	}
	return m.puzzle.board
}

func (m *Match) Rack() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.puzzle == nil {
		return ""
	}
	return m.puzzle.rack
}

func (m *Match) SolutionCount() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.puzzle == nil {
		return 0
	}
	return len(m.puzzle.solutions)
}

func (m *Match) SolutionAt(i int) *Solution {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.puzzle == nil || m.phase == PhaseSolving || i < 0 || i >= len(m.puzzle.solutions) {
		return nil
	}
	s := m.puzzle.solutions[i]
	return &s
}

func (m *Match) Outcome() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.outcome
}

func (m *Match) Answer() *Solution {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.answer == nil {
		return nil
	}
	a := *m.answer
	return &a
}

func (m *Match) LastError() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.lastErr
}
