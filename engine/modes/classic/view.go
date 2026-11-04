package classic

import (
	"lexico/engine/internal/core"
)

func (m *Match) ViewFor(first bool) (string, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	game, err := m.game.viewFor(first)
	if err != nil {
		return "", err
	}
	return core.JSON(m.saved(game))
}

func (m *Match) SpectatorView() (string, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	game, err := m.game.openView()
	if err != nil {
		return "", err
	}
	return core.JSON(m.saved(game))
}

func (m *Match) Forfeit(first bool) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.closed || m.game.Status().Over {
		return
	}
	m.chargeTime()
	m.game.forfeit(first)
	m.afterChange()
}

func (c *Game) viewFor(first bool) (string, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.over() {
		return c.fullSave()
	}
	viewer := c.humanIdx
	if !first {
		viewer = 1 - c.humanIdx
	}
	history, err := core.RedactedHistoryJSON(c.g, viewer)
	if err != nil {
		return "", err
	}
	return core.JSON(c.withoutRecord(c.saved(history)))
}

func (c *Game) openView() (string, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	history, err := core.HistoryJSON(c.g)
	if err != nil {
		return "", err
	}
	return core.JSON(c.withoutRecord(c.saved(history)))
}

func (c *Game) fullSave() (string, error) {
	history, err := core.HistoryJSON(c.g)
	if err != nil {
		return "", err
	}
	return core.JSON(c.saved(history))
}

func (c *Game) withoutRecord(s savedGame) savedGame {
	log := *c.log
	log.Turns = nil
	s.Log, s.RecordPath = &log, ""
	return s
}

func (c *Game) forfeit(first bool) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.over() {
		return
	}
	c.lostOnTime, c.rivalLostOnTime = first, !first
	c.finish()
}
