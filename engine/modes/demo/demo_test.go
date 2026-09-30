package demo

import (
	"encoding/json"
	"testing"

	"lexico/engine/internal/testenv"
)

func TestPlay(t *testing.T) {
	testenv.Init(t)
	var placements []string
	decodeGame(t, Play, &placements)
	t.Log(placements[:5])
}

func TestPlayWithScores(t *testing.T) {
	testenv.Init(t)
	var placements []ScoredPlacement
	decodeGame(t, PlayWithScores, &placements)
	for _, p := range placements {
		if p.Placement == "" || p.Score <= 0 {
			t.Fatalf("colocacion sin jugada o sin puntos: %+v", p)
		}
	}
}

func decodeGame[T any](t *testing.T, play func() (string, error), placements *[]T) {
	t.Helper()
	text, err := play()
	if err != nil {
		t.Fatal(err)
	}
	if err := json.Unmarshal([]byte(text), placements); err != nil || len(*placements) < 5 {
		t.Fatalf("%v %s", err, text)
	}
}
