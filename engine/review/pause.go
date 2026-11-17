package review

import "lexico/engine/internal/endgame"

func SetPause(f func()) {
	endgame.SetPause(f)
}
