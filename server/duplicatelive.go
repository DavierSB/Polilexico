package main

import "encoding/json"

func (r *duplicateRoom) progress(g liveGame) liveGame {
	view, err := r.table.View(r.seats[0].name)
	var t tableSummary
	if err != nil || json.Unmarshal([]byte(view), &t) != nil || len(t.Rounds) == 0 {
		return g
	}
	totals := t.totals()
	g.Round = len(t.Rounds)
	g.Leader, g.Tied = leaderOf(g.Players, totals)
	if e := efficiency(totals[g.Leader], totals[reservedName]); e != nil {
		g.Efficiency = int(*e*100 + 0.5)
	}
	return g
}

func leaderOf(players []string, totals map[string]int) (string, int) {
	leader, tied := "", 0
	for _, name := range players {
		switch {
		case leader == "" || totals[name] > totals[leader]:
			leader, tied = name, 1
		case totals[name] == totals[leader]:
			tied++
		}
	}
	return leader, tied
}
