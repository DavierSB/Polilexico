package review

func (g *Game) identify(log gameLog) {
	g.MeCountry, g.OpponentCountry = side(log.Countries, 0), side(log.Countries, 1)
	g.MeID, g.OpponentID = side(log.IDs, 0), side(log.IDs, 1)
}

func side(values []string, i int) string {
	if i < len(values) {
		return values[i]
	}
	return ""
}
