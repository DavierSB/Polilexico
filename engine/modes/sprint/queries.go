package sprint

// Lo que la plataforma lee de la serie en curso.

// Phase: la fase de la serie (PhaseSearching, PhaseSolving...).
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

// MaxLives: las vidas con que empezo la serie.
func (m *Match) MaxLives() int {
	return m.maxLives
}

// Lives: las vidas que quedan.
func (m *Match) Lives() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.lives
}

// Solved: las manos resueltas.
func (m *Match) Solved() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.solved
}

// Posed: las manos propuestas hasta ahora (la que esta en juego incluida).
func (m *Match) Posed() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.posed
}

// TotalMs: el tiempo de la serie.
func (m *Match) TotalMs() int64 {
	return m.totalTime.Milliseconds()
}

// RemainingMs: lo que queda en el reloj de la serie.
func (m *Match) RemainingMs() int64 {
	m.mu.Lock()
	defer m.mu.Unlock()
	return max(m.clock.Left(), 0).Milliseconds()
}

// Board: el tablero de la mano (en el formato de engine.BestMoves), o "" mientras se busca.
func (m *Match) Board() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.puzzle == nil {
		return ""
	}
	return m.puzzle.board
}

// Rack: el atril de la mano ("A CH E ?"), o "" mientras se busca.
func (m *Match) Rack() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.puzzle == nil {
		return ""
	}
	return m.puzzle.rack
}

// SolutionCount: cuantos scrabbles admite la mano (0 mientras se busca).
func (m *Match) SolutionCount() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.puzzle == nil {
		return 0
	}
	return len(m.puzzle.solutions)
}

// SolutionAt: el scrabble i de la mano (0 = el de mas puntos), solo con la mano ya cerrada;
// si no, nil.
func (m *Match) SolutionAt(i int) *Solution {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.puzzle == nil || m.phase == PhaseSolving || i < 0 || i >= len(m.puzzle.solutions) {
		return nil
	}
	s := m.puzzle.solutions[i]
	return &s
}

// Outcome: como se cerro la ultima mano (OutcomeSolved...), o "" si no hay mano cerrada.
func (m *Match) Outcome() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.outcome
}

// Answer: tu scrabble, si resolviste la ultima mano; si no, nil.
func (m *Match) Answer() *Solution {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.answer == nil {
		return nil
	}
	a := *m.answer
	return &a
}

// LastError: por que fallo la busqueda ("" si no fallo).
func (m *Match) LastError() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.lastErr
}
