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

type Game struct {
	mu               sync.Mutex
	g                *game.Game
	bot              *bot.BotTurnPlayer
	botName          string
	humanIdx         int
	moves            []*Move
	log              *gameLog
	lostOnTime       bool
	invalidLosesTurn bool
	recordPath       string
}

func Bots() string {
	return strings.Join(core.BotNames, ",")
}

func Start(botName string) (*Game, error) {
	g, err := core.NewGame(humanName, botName)
	if err != nil {
		return nil, err
	}
	humanIdx := drawFirstPlayer(g)
	g.StartGame()
	return newGame(g, botName, humanIdx, newGameLog(botName))
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
	b, err := core.NewBot(g, botName)
	if err != nil {
		return nil, err
	}
	return &Game{g: g, bot: b, botName: botName, humanIdx: humanIdx, log: log}, nil
}

func drawFirstPlayer(g *game.Game) int {
	if rand.Intn(2) == 0 {
		return 0
	}
	g.FlipPlayers()
	return 1
}

func newGameLog(botName string) *gameLog {
	return &gameLog{StartedAt: time.Now(), HumanName: humanName, BotName: botName}
}
