// Package review lee el registro de una partida terminada (el -log.json que escriben classic y
// duplicate, el mismo que revisan las herramientas de la PC) para revisarla turno a turno.
package review

import (
	"encoding/json"
	"os"
)

// Las modalidades, en Game.Mode.
const (
	ModeClassic   = "classic"
	ModeDuplicate = "duplicate"
	ModeEndgame   = "endgame" // Finales: una clasica que empieza en un final
)

// Game es una partida terminada. Opponent es el bot en clasica y "Máster" en duplicada; Outcome
// es "win", "loss" o "tie" para ti; Hits, solo en duplicada, los turnos en que igualaste al master.
// StartTurn es el turno (desde 0) en que empieza la revision: en Finales, el primero que jugaste
// tu; en las demas, 0. De tus colocaciones (en Finales, desde StartTurn): cuantas fueron scrabble (las 7 fichas), la palabra que mas puntos hizo y
// la mas larga (palabras completas, con las letras del tablero que atraviesan).
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

// Open lee el registro del archivo `path`.
func Open(path string) (*Game, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	return Parse(string(data))
}

// Parse lee un registro, de clasica (o Finales) o de duplicada.
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

// TurnCount: cuantos turnos tiene la partida.
func (g *Game) TurnCount() int {
	return len(g.turns)
}

// TurnAt devuelve el turno i (0 = el primero), o nil si no existe.
func (g *Game) TurnAt(i int) *Turn {
	if i < 0 || i >= len(g.turns) {
		return nil
	}
	return g.turns[i]
}
