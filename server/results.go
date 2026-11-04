package main

import (
	"context"
	"log"
	"time"

	"lexico/engine/modes/classic"
)

const bingoTiles = 7

type bests struct {
	bingo int
	play  int
}

func record(st store, r result) {
	go func() {
		ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		if err := st.Record(ctx, r); err != nil {
			log.Printf("no se guardó la partida: %v", err)
		}
	}()
}

func classicResult(r *classicRoom) result {
	g := r.match.Game()
	res := g.Result()
	best := classicBests(g)
	first := placing{UserID: r.seats[0].player.ID, Name: r.seats[0].name, Score: res.HumanScore,
		Result: outcomeResult(res.Outcome), BestBingo: best[0].bingo, BestPlay: best[0].play}
	second := placing{UserID: r.seats[1].player.ID, Name: r.seats[1].name, Score: res.BotScore,
		Result: reversed(first.Result), BestBingo: best[1].bingo, BestPlay: best[1].play}
	first.Place, second.Place = placeOf(first.Result), placeOf(second.Result)
	return result{Mode: modeClassic, StartedAt: r.started, Players: []placing{first, second},
		Settings: map[string]any{"timeMs": r.timeMs, "overtimeMs": r.overtimeMs}}
}

func classicBests(g *classic.Game) [2]bests {
	var out [2]bests
	for i := range g.MoveCount() {
		m := g.MoveAt(i)
		side := 1
		if m.ByHuman {
			side = 0
		}
		if m.Kind == "play" {
			out[side] = better(out[side], m.Score, m.TileCount)
		}
	}
	return out
}

func better(b bests, score, tiles int) bests {
	b.play = max(b.play, score)
	if tiles == bingoTiles {
		b.bingo = max(b.bingo, score)
	}
	return b
}

func reversed(outcome string) string {
	switch outcome {
	case "win":
		return "loss"
	case "loss":
		return "win"
	}
	return outcome
}

func placeOf(outcome string) int {
	if outcome == "loss" {
		return 2
	}
	return 1
}
