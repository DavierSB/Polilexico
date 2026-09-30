package sprint

func (m *Match) afterChange() {
	m.clock.Sync(!m.paused && !m.closed && m.phase == PhaseSolving, m.onTimeUp)
	m.notifier.Notify()
}

func (m *Match) search() {
	m.phase = PhaseSearching
	m.puzzle, m.answer, m.outcome = nil, nil, ""
	go m.await()
}

func (m *Match) await() {
	select {
	case f := <-m.hunter.found:
		m.receive(f)
	case <-m.hunter.done:
	}
}

func (m *Match) receive(f found) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.closed {
		return
	}
	if f.err != nil {
		m.lastErr, m.phase = f.err.Error(), PhaseFinished
	} else {
		m.pose(f.puzzle)
	}
	m.afterChange()
}

func (m *Match) pose(p *puzzle) {
	m.puzzle, m.phase = p, PhaseSolving
	m.posed++
}

func (m *Match) reveal(outcome string, answer *Solution) {
	m.outcome, m.answer = outcome, answer
	m.phase = PhaseRevealed
	switch outcome {
	case OutcomeSolved:
		m.solved++
	case OutcomeGaveUp, OutcomeInvalid:
		m.lives--
		if m.lives <= 0 {
			m.finish()
		}
	case OutcomeTimeout:
		m.finish()
	}
}

func (m *Match) finish() {
	m.phase = PhaseFinished
	m.hunter.stop()
}

func (m *Match) onTimeUp() {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.paused || m.closed {
		return
	}
	if m.phase == PhaseSolving && m.clock.Left() <= 0 {
		m.reveal(OutcomeTimeout, nil)
	}
	m.afterChange()
}
