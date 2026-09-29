package core

// Resultado de una partida para el humano, tal como llega a Android en Result.Outcome.
const (
	OutcomeWin  = "win"
	OutcomeLoss = "loss"
	OutcomeTie  = "tie"
)

// Outcome compara los puntos del humano con los del rival.
func Outcome(human, rival int) string {
	switch {
	case human > rival:
		return OutcomeWin
	case human < rival:
		return OutcomeLoss
	}
	return OutcomeTie
}
