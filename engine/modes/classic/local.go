package classic

import (
	"lexico/engine/events"
	"lexico/engine/internal/core"
)

const ModeLocal = "local"

func NewLocalMatch(first, second string, starter int, timeMs, overtimeMs int64, invalidLosesTurn bool, l events.Listener) (*Match, error) {
	g, err := StartLocal(first, second, starter)
	if err != nil {
		return nil, err
	}
	return newMatch(g, timeMs, overtimeMs, invalidLosesTurn, l), nil
}

func StartLocal(first, second string, starter int) (*Game, error) {
	g, err := core.NewGame(first, second)
	if err != nil {
		return nil, err
	}
	humanIdx := pickFirstPlayer(g, starter)
	g.StartGame()
	log := newGameLog(first, second)
	log.Mode = ModeLocal
	return newGame(g, second, humanIdx, log)
}

func (l *gameLog) isLocal() bool {
	return l.Mode == ModeLocal || l.Local
}

func (c *Game) Local() bool {
	return c.local
}

func (c *Game) Host() string {
	return c.log.HumanName
}

func (c *Game) keepRecord() {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.over() && c.recordPath == "" {
		c.finish()
	}
}

func engineBot(botName string, local bool) string {
	if local {
		return core.DefaultBot
	}
	return botName
}

func LoadMirror(text string, paused bool, l events.Listener) (*Match, error) {
	m, err := LoadMatch(text, l)
	if err != nil {
		return nil, err
	}
	m.mu.Lock()
	m.mirror = true
	m.mu.Unlock()
	m.game.keepRecord()
	if !paused {
		m.Resume()
	}
	return m, nil
}
