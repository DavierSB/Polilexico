package classic

import (
	"math/rand"
	"strings"
	"sync"
	"time"

	"github.com/domino14/macondo/ai/bot"
	"github.com/domino14/macondo/game"

	"lexico/engine/internal/core"
)

const humanName = "Tú"

const (
	StarterAny   = 0
	StarterHuman = 1
	StarterBot   = 2
)

type Game struct {
	mu               sync.Mutex
	g                *game.Game
	bot              *bot.BotTurnPlayer
	botName          string
	humanIdx         int
	moves            []*Move
	log              *gameLog
	lostOnTime       bool
	rivalLostOnTime  bool
	local            bool
	ending           *Ending
	timePenalties    [2]int
	invalidLosesTurn bool
	recordPath       string
}

func Bots() string {
	return strings.Join(core.BotNames, ",")
}

func Start(botName string, starter int) (*Game, error) {
	g, err := core.NewGame(humanName, botName)
	if err != nil {
		return nil, err
	}
	humanIdx := pickFirstPlayer(g, starter)
	g.StartGame()
	return newGame(g, botName, humanIdx, newGameLog(humanName, botName))
}

func (c *Game) SetInvalidPlayLosesTurn(on bool) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.invalidLosesTurn = on
}

func (c *Game) Opponent() string {
	return c.botName
}

func (c *Game) HumanStarts() bool {
	return c.humanIdx == 0
}

func newGame(g *game.Game, botName string, humanIdx int, log *gameLog) (*Game, error) {
	local := log.isLocal()
	b, err := core.NewBot(g, engineBot(botName, local))
	if err != nil {
		return nil, err
	}
	g.SetMaxScorelessTurns(noScorelessEnd)
	return &Game{g: g, bot: b, botName: botName, humanIdx: humanIdx, log: log, local: local}, nil
}

func pickFirstPlayer(g *game.Game, starter int) int {
	if starter == StarterHuman || starter != StarterBot && rand.Intn(2) == 0 {
		return 0
	}
	g.FlipPlayers()
	return 1
}

func newGameLog(human, botName string) *gameLog {
	return &gameLog{StartedAt: time.Now(), HumanName: human, BotName: botName}
}
