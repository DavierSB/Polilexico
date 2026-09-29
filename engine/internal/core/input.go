package core

import (
	"errors"
	"fmt"
	"strings"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"
	"github.com/domino14/macondo/turnplayer"
	"github.com/domino14/word-golib/tilemapping"
)

var (
	errEmptyInput      = errors.New("escribe una jugada")
	errPlacementFormat = errors.New("formato: COORDENADA PALABRA (ej: h8 CASA = horizontal, 8h CASA = vertical)")
	errExchangeFormat  = errors.New("uso: cambiar FICHAS (ej: cambiar ABC, o cambiar ? para el comodín)")
)

// Palabras con que se pide pasar o cambiar, en español y como en el shell de macondo.
var (
	passWords     = map[string]bool{"pasar": true, "pass": true}
	exchangeWords = map[string]bool{"cambiar": true, "exchange": true}
)

// InvalidWordsError: la jugada cabe en el tablero pero forma palabras que no estan en el
// diccionario. Segun el modo cuenta como turno perdido o se rechaza. Como en el tablero de
// verdad, el mensaje no dice cuales son.
type InvalidWordsError struct {
	Play  Play // la jugada intentada, con Kind = KindInvalid y 0 puntos
	Words []string
}

func (e *InvalidWordsError) Error() string {
	return "Jugada inválida"
}

// AsInvalidWords dice si err es un InvalidWordsError y lo devuelve.
func AsInvalidWords(err error) (*InvalidWordsError, bool) {
	var e *InvalidWordsError
	return e, errors.As(err, &e)
}

// ParseInput convierte lo que escribe el jugador en una jugada de macondo, igual que el
// bucle de consola de la PC: "h8 CASA" / "8h CASA", "pasar" o "cambiar ABC". Las colocaciones
// se comprueban contra el tablero y el diccionario.
func ParseInput(g *game.Game, player int, input string) (*move.Move, error) {
	fields := strings.Fields(input)
	if len(fields) == 0 {
		return nil, errEmptyInput
	}
	tp := &turnplayer.BaseTurnPlayer{Game: g}
	switch command := strings.ToLower(fields[0]); {
	case passWords[command]:
		return tp.NewPassMove(player)
	case exchangeWords[command]:
		return parseExchange(tp, player, fields)
	}
	return parsePlacement(tp, player, fields)
}

// IsExchange dice si input pide un cambio de fichas.
func IsExchange(input string) bool {
	fields := strings.Fields(input)
	return len(fields) > 0 && exchangeWords[strings.ToLower(fields[0])]
}

// IsWord dice si w esta en el diccionario.
func IsWord(g *game.Game, w tilemapping.MachineWord) bool {
	return g.ValidateWords(g.Lexicon(), []tilemapping.MachineWord{w}) == nil
}

func parseExchange(tp *turnplayer.BaseTurnPlayer, player int, fields []string) (*move.Move, error) {
	if len(fields) < 2 {
		return nil, errExchangeFormat
	}
	return tp.NewExchangeMove(player, ExpandEnhe(strings.ToUpper(fields[1])))
}

// PlacementMove lee una colocacion ("h8 CASA") sin mirar el diccionario: la jugada tal cual,
// con sus puntos. Falla si no cabe en el tablero.
func PlacementMove(g *game.Game, player int, input string) (*move.Move, error) {
	return uncheckedPlacement(&turnplayer.BaseTurnPlayer{Game: g}, player, strings.Fields(input))
}

// parsePlacement lee "h8 CASA" y comprueba sus palabras contra el diccionario.
func parsePlacement(tp *turnplayer.BaseTurnPlayer, player int, fields []string) (*move.Move, error) {
	m, err := uncheckedPlacement(tp, player, fields)
	if err != nil {
		return nil, err
	}
	if err := checkWords(tp.Game, m); err != nil {
		return nil, err
	}
	return m, nil
}

func uncheckedPlacement(tp *turnplayer.BaseTurnPlayer, player int, fields []string) (*move.Move, error) {
	if len(fields) < 2 {
		return nil, errPlacementFormat
	}
	return placementMove(tp, player, fields[0], fields[1])
}

// La palabra no se pasa a mayusculas: la minuscula es como macondo designa el comodin.
func placementMove(tp *turnplayer.BaseTurnPlayer, player int, coords, word string) (*move.Move, error) {
	row, col, vertical, err := ParseCoords(coords)
	if err != nil {
		return nil, err
	}
	m, err := tp.NewPlacementMove(player, move.ToBoardGameCoords(row, col, vertical), ExpandEnhe(word), false)
	if err != nil {
		return nil, fmt.Errorf("jugada inválida: %w", err)
	}
	return m, nil
}

func checkWords(g *game.Game, m *move.Move) error {
	words, err := g.Board().FormedWords(m)
	if err != nil {
		return fmt.Errorf("jugada inválida: %w", err)
	}
	if bad := invalidWords(g, words); len(bad) > 0 {
		return &InvalidWordsError{Play: invalidPlay(m), Words: bad}
	}
	return nil
}

// El motor solo dice "one or more words are invalid"; aqui se nombran una a una.
func invalidWords(g *game.Game, words []tilemapping.MachineWord) []string {
	var bad []string
	for _, w := range words {
		if !IsWord(g, w) {
			bad = append(bad, withoutBrackets(w.UserVisible(g.Alphabet())))
		}
	}
	return bad
}

func invalidPlay(m *move.Move) Play {
	p := PlayOf(m)
	p.Kind, p.Score = KindInvalid, 0
	return p
}
