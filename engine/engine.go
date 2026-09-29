// Package engine es la API pura del motor de Woogles (macondo) para Android: iniciar el motor,
// consultar el diccionario, puntuar colocaciones y analizar posiciones. Los modos de juego estan en modes/.
//
// Aqui no se reimplementa nada de Scrabble: reglas, diccionario y bots salen tal cual de
// macondo. Todo lo exportado usa solo tipos que gomobile sabe pasar a Java.
package engine

import (
	"strings"

	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
)

// Init prepara el motor; hay que llamarlo antes que cualquier otra funcion. dataDir es la
// carpeta "data" de macondo (lexica/gaddag/FILE2017.*, letterdistributions/spanish,
// strategy/default); savesDir, donde se escriben los registros de las partidas terminadas.
func Init(dataDir, savesDir string) error {
	return core.Init(dataDir, savesDir)
}

// IsValidWord dice si una palabra esta en FILE2017 (el "check" del shell de macondo).
// Acepta [N] como Ñ y los digrafos entre corchetes ("[CH]").
func IsValidWord(word string) (bool, error) {
	g, err := core.NewGame("a", "b")
	if err != nil {
		return false, err
	}
	w, err := tilemapping.ToMachineWord(normalizeWord(word), g.Alphabet())
	if err != nil {
		return false, err
	}
	return core.IsWord(g, w), nil
}

// BestMoves es el "gen" del shell de macondo sobre una posicion cualquiera: las n mejores
// jugadas segun HastyBot, como JSON [{"coords","description","score","equity"}].
//
// board: las 225 casillas (fila A..O, columna 1..15) separadas por espacios; "." = vacia,
// minuscula = comodin. rack: "AEIRST?" (los digrafos como [CH]).
func BestMoves(board, rack string, n int) (string, error) {
	g, err := gameFromPosition(board, rack)
	if err != nil {
		return "", err
	}
	hasty, err := core.NewBot(g, core.DefaultBot)
	if err != nil {
		return "", err
	}
	return core.JSON(core.Candidates(hasty.GenerateMoves(n)))
}

// PlacementScore: los puntos que valdria la colocacion placement ("H8 CA.A", "8H [CH]e": "."
// = letra ya en el tablero, minuscula = comodin) sobre board (como en BestMoves), sin mirar el
// diccionario: son los puntos que se ven mientras se coloca. Falla si la jugada no cabe: fuera de
// linea, sin tocar otras fichas, o sin pasar por el centro en la primera.
func PlacementScore(board, placement string) (int, error) {
	g, err := gameFromPosition(board, placedRack(placement))
	if err != nil {
		return 0, err
	}
	m, err := core.PlacementMove(g, 0, placement)
	if err != nil {
		return 0, err
	}
	return m.Score(), nil
}

func normalizeWord(word string) string {
	return core.ExpandEnhe(strings.ToUpper(strings.TrimSpace(word)))
}
