// Package duplicate es el Scrabble duplicado contra el máster (HastyBot ordenando por
// puntos), con las reglas FISF de ./duplicate.sh en la PC: todos juegan el mismo atril y solo
// la jugada del máster avanza el tablero.
//
// Cada turno: DrawRack muestra el atril, Validate comprueba tu jugada, Confirm la anota (o
// TimeOut si se acaba el reloj) y devuelve el Turn con la jugada del máster.
package duplicate

import (
	"sync"
	"time"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"
	"github.com/domino14/macondo/movegen"

	"lexico/engine/internal/core"
)

// Tiempos de la duplicada. Los lleva Match, la partida en marcha.
const (
	TurnSeconds        = 200 // 3:20 por turno
	CancelSeconds      = 10  // para deshacer una jugada antes de confirmarla
	InvalidRackSeconds = 5   // se muestra la mano invalida antes de volver a sacar
)

// La partida tiene dos plazas porque macondo lo exige; solo juega la del máster (su atril
// evoluciona como un unico linaje: el reliquat de cada turno).
const masterIdx = 0

// Game es una partida duplicada. Todos sus metodos se pueden llamar desde cualquier hilo.
type Game struct {
	mu     sync.Mutex
	g      *game.Game
	master *movegen.GordonGenerator
	// Turno en curso: su numero, el atril, el tablero de partida y las jugadas posibles
	// (la mejor primero). plays es nil entre turnos.
	turn        int
	rack        string
	boardBefore string
	plays       []*move.Move
	// Marcador.
	masterTotal, humanTotal, hits int
	turns                         []*Turn
	log                           *gameLog
	ended                         bool // la bolsa ya no permitia formar un atril
	invalidLosesTurn              bool
	recordPath                    string
}

// Start empieza una partida duplicada.
func Start() (*Game, error) {
	g, err := core.NewGame("Máster", "Máster 2")
	if err != nil {
		return nil, err
	}
	g.StartGame()
	return newGame(g, &gameLog{StartedAt: time.Now()})
}

// SetInvalidPlayLosesTurn: con true, una jugada con palabras no validas se anota como turno
// perdido (0 puntos, modo "single"); con false se rechaza y puedes corregirla (modo "void").
func (d *Game) SetInvalidPlayLosesTurn(on bool) {
	d.mu.Lock()
	defer d.mu.Unlock()
	d.invalidLosesTurn = on
}

func newGame(g *game.Game, log *gameLog) (*Game, error) {
	seatMaster(g)
	master, err := core.NewMaster(g)
	if err != nil {
		return nil, err
	}
	return &Game{g: g, master: master, log: log}, nil
}

// Vacia la plaza que no juega y deja el turno en la del máster.
func seatMaster(g *game.Game) {
	g.ThrowRacksInFor(1 - masterIdx)
	g.SetPlayerOnTurn(masterIdx)
}
