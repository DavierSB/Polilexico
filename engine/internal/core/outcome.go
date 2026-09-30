package core

const (
	OutcomeWin  = "win"
	OutcomeLoss = "loss"
	OutcomeTie  = "tie"
)

func Outcome(human, rival int) string {
	switch {
	case human > rival:
		return OutcomeWin
	case human < rival:
		return OutcomeLoss
	}
	return OutcomeTie
}
