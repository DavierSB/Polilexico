package core

import (
	"fmt"

	"github.com/domino14/macondo/ai/bot"
	"github.com/domino14/macondo/game"
	pb "github.com/domino14/macondo/gen/api/proto/macondo"
	"github.com/domino14/macondo/movegen"
)

// DefaultBot es el bot de las posiciones sueltas, la demo y el máster de la duplicada.
const DefaultBot = "HastyBot"

// BotNames: los bots de Woogles.io que se pueden usar, de menor a mayor nivel.
var BotNames = []string{"BeginnerBot", "BasicBot", "BetterBot", "STEEBot", "HastyBot"}

// Niveles de macondo de cada bot, como los define la web (liwords-ui/src/lobby/bots.ts) para
// lexicos sin sublexico de palabras comunes, como FILE2017. BestBot (simulacion) no: es
// demasiado pesado para un telefono.
var botLevels = map[string]pb.BotRequest_BotCode{
	"BeginnerBot": pb.BotRequest_LEVEL1_PROBABILISTIC,
	"BasicBot":    pb.BotRequest_LEVEL2_PROBABILISTIC,
	"BetterBot":   pb.BotRequest_LEVEL3_PROBABILISTIC,
	"STEEBot":     pb.BotRequest_LEVEL4_PROBABILISTIC,
	"HastyBot":    pb.BotRequest_HASTY_BOT,
}

// NewBot crea uno de los bots de BotNames jugando en g.
func NewBot(g *game.Game, name string) (*bot.BotTurnPlayer, error) {
	level, ok := botLevels[name]
	if !ok {
		return nil, fmt.Errorf("bot desconocido: %s", name)
	}
	return newBotPlayer(g, level)
}

// NewMaster crea el generador del máster de la duplicada: el de HastyBot, ordenando las
// jugadas por puntos en lugar de por equity.
func NewMaster(g *game.Game) (*movegen.GordonGenerator, error) {
	b, err := newBotPlayer(g, botLevels[DefaultBot])
	if err != nil {
		return nil, err
	}
	gen := generatorOf(b)
	gen.SetSortingParameter(movegen.SortByScore)
	return gen, nil
}

func newBotPlayer(g *game.Game, level pb.BotRequest_BotCode) (*bot.BotTurnPlayer, error) {
	b, err := bot.NewBotTurnPlayerFromGame(g, &bot.BotConfig{Config: *cfg}, level)
	if err != nil {
		return nil, err
	}
	generatorOf(b).SetGame(g)
	return b, nil
}

func generatorOf(b *bot.BotTurnPlayer) *movegen.GordonGenerator {
	return b.MoveGenerator().(*movegen.GordonGenerator)
}
