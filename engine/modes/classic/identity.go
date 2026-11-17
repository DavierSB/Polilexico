package classic

func (m *Match) SetIdentity(first bool, country, id string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.game.setIdentity(first, country, id)
}

func (c *Game) setIdentity(first bool, country, id string) {
	c.mu.Lock()
	defer c.mu.Unlock()
	side := sideOf(first)
	c.log.Countries = withValue(c.log.Countries, side, country)
	c.log.IDs = withValue(c.log.IDs, side, id)
}

func sideOf(first bool) int {
	if first {
		return 0
	}
	return 1
}

func withValue(values []string, side int, value string) []string {
	if value == "" && len(values) == 0 {
		return values
	}
	out := make([]string, 2)
	copy(out, values)
	out[side] = value
	return out
}

func (l *gameLog) seenBy(guest bool) *gameLog {
	if !guest {
		return l
	}
	return l.swapped()
}

func (l gameLog) swapped() *gameLog {
	l.HumanName, l.BotName = l.BotName, l.HumanName
	l.FinalScores = [2]int{l.FinalScores[1], l.FinalScores[0]}
	l.Countries, l.IDs = reversed(l.Countries), reversed(l.IDs)
	return &l
}

func reversed(values []string) []string {
	if len(values) != 2 {
		return values
	}
	return []string{values[1], values[0]}
}
