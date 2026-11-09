package main

import "encoding/json"

func (r *duplicateRoom) progress(g liveGame) liveGame {
	view, err := r.table.View(r.seats[0].name)
	var t tableSummary
	if err != nil || json.Unmarshal([]byte(view), &t) != nil {
		return g
	}
	g.Round = len(t.Rounds) + 1
	totals := t.totals()
	leader, best := "", -1
	for name, total := range totals {
		if name != reservedName && total > best {
			leader, best = name, total
		}
	}
	if e := efficiency(best, totals[reservedName]); e != nil && leader != "" {
		g.Leader, g.Efficiency = leader, int(*e*100+0.5)
	}
	return g
}
