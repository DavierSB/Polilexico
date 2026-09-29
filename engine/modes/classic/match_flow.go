package classic

// El ritmo de la partida en marcha: tras cada cambio se ponen al dia los relojes, la alarma de
// tu tiempo y el turno del bot, y se avisa a la plataforma. Todo con m.mu tomado, salvo lo que
// piensa el bot, que va en su propio hilo.

// afterChange pone la partida al dia tras cualquier cambio.
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

// runClocks deja corriendo el reloj de quien tiene el turno, o ninguno.
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

// scheduleTimeout programa la derrota por tiempo para cuando se te acabe el descuento.
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

// botTurn elige la jugada del bot fuera del candado y la juega; si mientras tanto la partida se
// pauso, la descarta: el bot vuelve a pensarla al continuar.
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
