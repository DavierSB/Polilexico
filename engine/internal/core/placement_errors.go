package core

import (
	"errors"
	"strings"
)

var placementMessages = []struct{ cause, text string }{
	{"out of bounds", "La jugada se sale del tablero."},
	{"off of the board", "La jugada se sale del tablero."},
	{"play-through tile is incorrect", "La jugada no coincide con las fichas del tablero."},
	{"played-through marker", "La jugada no coincide con las fichas del tablero."},
	{"play through a letter already", "La jugada no coincide con las fichas del tablero."},
	{"center square", "La primera jugada debe pasar por la casilla central."},
	{"border a tile", "La jugada debe tocar alguna ficha del tablero."},
	{"place a new tile", "La jugada debe poner al menos una ficha nueva."},
	{"at least two letters", "La palabra debe tener al menos dos letras."},
	{"whole word", "Escribe la palabra completa, con las fichas del tablero."},
	{"not in rack", "Usaste una ficha que no está en tu atril."},
	{"designated blank", "Usaste una ficha que no está en tu atril."},
	{"not in your rack", "Usaste una ficha que no está en tu atril."},
	{"too many tiles", "La jugada tiene más fichas de las que tienes."},
	{"not found in alphabet", "La jugada tiene letras que no existen."},
	{"cannot convert", "La jugada tiene letras que no existen."},
}

var errPlacement = errors.New("La jugada no es válida.")

func placementError(err error) error {
	cause := strings.ToLower(err.Error())
	for _, m := range placementMessages {
		if strings.Contains(cause, m.cause) {
			return errors.New(m.text)
		}
	}
	return errPlacement
}
