package core

import (
	"fmt"
	"regexp"
	"strconv"
	"strings"

	"github.com/domino14/macondo/move"
)

// Notacion FISE: "H8" = fila H, columna 8, horizontal; "8H" = lo mismo en vertical.
var (
	horizontalCoords = regexp.MustCompile(`^(?i)([A-O])([0-9]{1,2})$`)
	verticalCoords   = regexp.MustCompile(`^([0-9]{1,2})(?i)([A-O])$`)
)

// ParseCoords lee unas coordenadas FISE (fila y columna desde 0).
func ParseCoords(token string) (row, col int, vertical bool, err error) {
	token = strings.TrimSpace(token)
	if m := horizontalCoords.FindStringSubmatch(token); m != nil {
		return coordsOf(token, m[1], m[2], false)
	}
	if m := verticalCoords.FindStringSubmatch(token); m != nil {
		return coordsOf(token, m[2], m[1], true)
	}
	return 0, 0, false, fmt.Errorf("coordenada inválida %q (ej: h8 para horizontal, 8h para vertical)", token)
}

// FormatCoords escribe unas coordenadas en notacion FISE.
func FormatCoords(row, col int, vertical bool) string {
	if vertical {
		return fmt.Sprintf("%d%c", col+1, 'A'+row)
	}
	return fmt.Sprintf("%c%d", 'A'+row, col+1)
}

// MoveCoords: las coordenadas FISE de una colocacion ("" si no lo es).
func MoveCoords(m *move.Move) string {
	if m.Action() != move.MoveTypePlay {
		return ""
	}
	return FormatCoords(m.CoordsAndVertical())
}

// ExpandEnhe: [N] = Ñ y [n] = comodin como Ñ, para teclados sin Ñ.
func ExpandEnhe(word string) string {
	return enhe.Replace(word)
}

var enhe = strings.NewReplacer("[N]", "Ñ", "[n]", "ñ")

func coordsOf(token, rowLetter, colDigits string, vertical bool) (int, int, bool, error) {
	col, _ := strconv.Atoi(colDigits)
	if col < 1 || col > 15 {
		return 0, 0, false, fmt.Errorf("columna inválida en %q (debe ser 1-15)", token)
	}
	return int(strings.ToUpper(rowLetter)[0] - 'A'), col - 1, vertical, nil
}

// Los digrafos van entre corchetes en macondo ("[CH]"); en la interfaz, sin ellos.
func withoutBrackets(s string) string {
	return brackets.Replace(s)
}

var brackets = strings.NewReplacer("[", "", "]", "")
