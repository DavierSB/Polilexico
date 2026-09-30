package review

import (
	"strings"

	"lexico/engine/internal/core"
)

type gameLog struct {
	StartedAt   string    `json:"started_at"`
	HumanName   string    `json:"human_name"`
	BotName     string    `json:"bot_name"`
	Turns       []turnLog `json:"turns"`
	FinalScores [2]int    `json:"final_scores"`
	Winner      string    `json:"winner"`
	FinalMaster int       `json:"final_master"`
	FinalHuman  int       `json:"final_human"`
	FinalHits   int       `json:"final_aciertos"`
	Mode        string    `json:"mode"`
	StartTurn   int       `json:"start_turn"`
}

type turnLog struct {
	TurnNum    int              `json:"turn_num"`
	PlayerName string           `json:"player_name"`
	Rack       string           `json:"rack"`
	GenList    []core.Candidate `json:"gen_list"`
	ActualPlay core.Candidate   `json:"actual_play"`
	TopPlays   []core.Candidate `json:"top_plays"`
	MasterPlay core.Candidate   `json:"master_play"`
	HumanPlay  core.Candidate   `json:"human_play"`
}

func classicGame(log gameLog) *Game {
	g := &Game{Mode: classicMode(log), StartedAt: log.StartedAt, Opponent: log.BotName,
		MyScore: log.FinalScores[0], OpponentScore: log.FinalScores[1], Outcome: classicOutcome(log),
		StartTurn: log.StartTurn}
	board := newGrid()
	for i, t := range log.Turns {
		g.turns = append(g.turns, classicTurn(t, board))
		if t.PlayerName == log.HumanName && i >= log.StartTurn {
			g.notePlay(board, t.ActualPlay)
		}
		board.play(t.ActualPlay.Description)
	}
	return g
}

func classicMode(log gameLog) string {
	if log.Mode == ModeEndgame {
		return ModeEndgame
	}
	return ModeClassic
}

func duplicateGame(log gameLog) *Game {
	g := &Game{Mode: ModeDuplicate, StartedAt: log.StartedAt, Opponent: "Máster", MyScore: log.FinalHuman,
		OpponentScore: log.FinalMaster, Outcome: core.Outcome(log.FinalHuman, log.FinalMaster), Hits: log.FinalHits}
	board := newGrid()
	for _, t := range log.Turns {
		g.turns = append(g.turns, duplicateTurn(t, board))
		g.notePlay(board, t.HumanPlay)
		board.play(t.MasterPlay.Description)
	}
	return g
}

func classicTurn(t turnLog, board *grid) *Turn {
	turn := newTurn(t, t.PlayerName, board, moves(t.GenList, true))
	turn.marks = []*Mark{turn.mark(t.PlayerName, move(t.ActualPlay, true))}
	return turn
}

func duplicateTurn(t turnLog, board *grid) *Turn {
	turn := newTurn(t, "", board, moves(t.TopPlays, false))
	turn.marks = []*Mark{turn.mark("Máster", move(t.MasterPlay, false)), turn.mark("Tú", move(t.HumanPlay, false))}
	return turn
}

func newTurn(t turnLog, player string, board *grid, candidates []*Move) *Turn {
	return &Turn{Number: t.TurnNum, Player: player, Rack: rackText(t.Rack), Board: board.text(), candidates: candidates}
}

func moves(candidates []core.Candidate, hasEquity bool) []*Move {
	out := make([]*Move, len(candidates))
	for i, c := range candidates {
		out[i] = move(c, hasEquity)
	}
	return out
}

func move(c core.Candidate, hasEquity bool) *Move {
	return &Move{Description: c.Description, Score: c.Score, Equity: c.Equity, HasEquity: hasEquity}
}

func classicOutcome(log gameLog) string {
	switch log.Winner {
	case log.HumanName:
		return core.OutcomeWin
	case log.BotName:
		return core.OutcomeLoss
	}
	return core.OutcomeTie
}

func rackText(rack string) string {
	if strings.Contains(rack, " ") {
		return rack
	}
	return strings.Join(plain(tokens(rack)), " ")
}
