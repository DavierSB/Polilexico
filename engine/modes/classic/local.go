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

func (c *Game) markMirrored() {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.mirrored = true
}

func (c *Game) keepRecord(as viewer) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.watched, c.guest = as == watcher, as == guest
	if c.over() && !core.RecordExists(c.recordPath) {
		c.finish()
	}
}

func engineBot(botName string, local bool) string {
	if local {
		return core.DefaultBot
	}
	return botName
}

type viewer int

const (
	host viewer = iota
	guest
	watcher
)

func LoadMirror(text string, paused bool, l events.Listener) (*Match, error) {
	return loadMirror(text, paused, l, host)
}

func LoadGuestMirror(text string, paused bool, l events.Listener) (*Match, error) {
	return loadMirror(text, paused, l, guest)
}

func LoadWatch(text string, paused bool, l events.Listener) (*Match, error) {
	return loadMirror(text, paused, l, watcher)
}

func loadMirror(text string, paused bool, l events.Listener, as viewer) (*Match, error) {
	m, err := LoadMatch(text, l)
	if err != nil {
		return nil, err
	}
	m.mu.Lock()
	m.mirror = true
	m.mu.Unlock()
	m.game.keepRecord(as)
	m.game.markMirrored()
	if !paused {
		m.Resume()
	}
	return m, nil
}
