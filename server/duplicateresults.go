package main

import (
	"encoding/json"

	"lexico/engine/modes/duplicate"
)

type tableSummary struct {
	Phase  string
	Rounds []struct {
		Plays []duplicate.TablePlay
	}
}

func (r *duplicateRoom) finalView() (tableSummary, bool) {
	view, err := r.table.View(r.seats[0].name)
	if err != nil {
		return tableSummary{}, false
	}
	var t tableSummary
	if json.Unmarshal([]byte(view), &t) != nil || t.Phase != duplicate.TableFinished {
		return tableSummary{}, false
	}
	return t, true
}

func duplicateResult(r *duplicateRoom, final tableSummary) result {
	totals, best := final.totals(), final.bests()
	players := make([]placing, len(r.seats))
	for i, s := range r.seats {
		players[i] = placing{UserID: s.player.ID, Name: s.name, Score: totals[s.name],
			Place: placeAmong(totals[s.name], totals), BestBingo: best[s.name].bingo, BestPlay: best[s.name].play,
			Efficiency: efficiency(totals[s.name], totals[reservedName])}
	}
	markWinners(players)
	return result{Mode: modeDuplicate, StartedAt: r.started, Players: players,
		Settings: map[string]any{"turnMs": r.turnMs, "rounds": r.rounds, "live": r.live}}
}

func (t tableSummary) totals() map[string]int {
	totals := map[string]int{}
	for _, round := range t.Rounds {
		for _, p := range round.Plays {
			totals[p.Name] = p.Total
		}
	}
	return totals
}

func (t tableSummary) bests() map[string]bests {
	out := map[string]bests{}
	for _, round := range t.Rounds {
		for _, p := range round.Plays {
			if p.Kind == "play" {
				out[p.Name] = better(out[p.Name], p.Points, p.TileCount)
			}
		}
	}
	return out
}

func placeAmong(total int, totals map[string]int) int {
	place := 1
	for name, other := range totals {
		if name != reservedName && other > total {
			place++
		}
	}
	return place
}

func markWinners(players []placing) {
	winners := 0
	for _, p := range players {
		if p.Place == 1 {
			winners++
		}
	}
	for i := range players {
		players[i].Result = resultOf(players[i].Place, winners)
	}
}

func resultOf(place, winners int) string {
	switch {
	case place > 1:
		return "loss"
	case winners > 1:
		return "draw"
	}
	return "win"
}

func efficiency(total, master int) *float64 {
	if master <= 0 {
		return nil
	}
	e := float64(total) / float64(master)
	return &e
}
