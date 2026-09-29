package sprint

// El ritmo de la serie: buscar, proponer la mano, cerrarla y volver a buscar. El reloj de la
// serie solo corre mientras se piensa una mano. Todo con m.mu tomado, salvo await, que espera a
// la busqueda sin bloquear la serie.

// afterChange pone el reloj al dia tras cualquier cambio y avisa a la plataforma.
func (m *Match) afterChange() {
	m.clock.Sync(!m.paused && !m.closed && m.phase == PhaseSolving, m.onTimeUp)
	m.notifier.Notify()
}

// search deja la mano anterior y espera, en otro hilo, el siguiente problema.
func (m *Match) search() {
	m.phase = PhaseSearching
	m.puzzle, m.answer, m.outcome = nil, nil, ""
	go m.await()
}

// await espera el problema que encuentre la busqueda (o a que se cierre la serie).
func (m *Match) await() {
	select {
	case f := <-m.hunter.found:
		m.receive(f)
	case <-m.hunter.done:
	}
}

// receive propone el problema encontrado; si la busqueda fallo, la serie termina.
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

// pose: a resolver la mano; el reloj sigue donde se quedo.
func (m *Match) pose(p *puzzle) {
	m.puzzle, m.phase = p, PhaseSolving
	m.posed++
}

// reveal cierra la mano: un punto si la resolviste; si te rendiste, una vida menos, y sin
// vidas se acabo. Si se agoto el reloj, se acabo sin perder vidas.
func (m *Match) reveal(outcome string, answer *Solution) {
	m.outcome, m.answer = outcome, answer
	m.phase = PhaseRevealed
	switch outcome {
	case OutcomeSolved:
		m.solved++
	case OutcomeGaveUp:
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

// onTimeUp: se agoto el reloj de la serie (si de verdad se agoto y la serie sigue en marcha).
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
