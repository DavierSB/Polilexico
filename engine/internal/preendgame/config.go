package preendgame

import (
	"time"

	"lexico/engine/internal/endgame"
)

type Config struct {
	Penalty    float64
	Endgame    endgame.Config
	ScanBudget time.Duration
}

var Default = Config{
	Penalty:    30,
	Endgame:    endgame.Default,
	ScanBudget: 2 * time.Second,
}
