package review

import "testing"

func TestPositionOfAnEndgameTurnIsAnEndgame(t *testing.T) {
	g := playedEndgame(t)
	turn := g.TurnAt(g.StartTurn)
	p := fromTurn(t, turn)
	if !p.Endgame || p.PreEndgame {
		t.Fatal("la posición del final no es un final")
	}
	if run(t, analysis(t, explorer(t, &Game{turns: []*Turn{p}})), &counter{}).LineCount() == 0 {
		t.Fatal("el final de la posición no tiene líneas")
	}
}

func TestPositionOfAPreEndgameTurnIsAPreEndgame(t *testing.T) {
	p := fromTurn(t, preEndgameGame(t).TurnAt(swallowsQ))
	if !p.PreEndgame || p.Endgame {
		t.Fatal("la posición del pre-final no es un pre-final")
	}
	if _, err := p.AnalyzePreEndgame(); err != nil {
		t.Fatal(err)
	}
}

func TestPositionRejectsImpossibleRacks(t *testing.T) {
	g := playedEndgame(t)
	if _, err := FromPosition(g.TurnAt(0).Board, "ZZZZ", 0, 0); err == nil {
		t.Fatal("aceptó cuatro zetas")
	}
}

func fromTurn(t *testing.T, turn *Turn) *Turn {
	t.Helper()
	p, err := FromPosition(turn.Board, turn.position.rack, turn.position.scores[0], turn.position.scores[1])
	if err != nil {
		t.Fatal(err)
	}
	return p
}
