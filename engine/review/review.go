package review

import (
	"encoding/json"
	"os"
)

const (
	ModeClassic   = "classic"
	ModeDuplicate = "duplicate"
	ModeEndgame   = "endgame"
)

type Game struct {
	Mode          string
	StartedAt     string
	Opponent      string
	MyScore       int
	OpponentScore int
	Outcome       string
	StartTurn     int
	Hits          int
	Bingos        int
	BestWord      string
	BestWordScore int
	LongestWord   string
	turns         []*Turn
}

func Open(path string) (*Game, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	return Parse(string(data))
}

func Parse(text string) (*Game, error) {
	var log gameLog
	if err := json.Unmarshal([]byte(text), &log); err != nil {
		return nil, err
	}
	if log.BotName != "" {
		return classicGame(log), nil
	}
	return duplicateGame(log), nil
}

func (g *Game) TurnCount() int {
	return len(g.turns)
}

func (g *Game) TurnAt(i int) *Turn {
	if i < 0 || i >= len(g.turns) {
		return nil
	}
	return g.turns[i]
}
