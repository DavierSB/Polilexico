package main

func tally(results []result, userID string) []statsRow {
	rows := map[string]*statsRow{}
	var order []string
	for _, r := range results {
		for _, p := range r.Players {
			if p.UserID != userID {
				continue
			}
			row := rows[r.Mode]
			if row == nil {
				row = &statsRow{Mode: r.Mode}
				rows[r.Mode] = row
				order = append(order, r.Mode)
			}
			count(row, p)
		}
	}
	out := make([]statsRow, len(order))
	for i, mode := range order {
		out[i] = averaged(*rows[mode])
	}
	return out
}

func count(row *statsRow, p placing) {
	row.Games++
	switch p.Result {
	case "win":
		row.Wins++
	case "loss":
		row.Losses++
	default:
		row.Draws++
	}
	row.BestBingo = max(row.BestBingo, p.BestBingo)
	row.BestPlay = max(row.BestPlay, p.BestPlay)
	row.AvgScore += float64(p.Score)
	if p.Efficiency != nil {
		sum := *p.Efficiency
		if row.AvgEfficiency != nil {
			sum += *row.AvgEfficiency
		}
		row.AvgEfficiency = &sum
	}
}

func averaged(row statsRow) statsRow {
	row.AvgScore /= float64(row.Games)
	if row.AvgEfficiency != nil {
		avg := *row.AvgEfficiency / float64(row.Games)
		row.AvgEfficiency = &avg
	}
	return row
}
