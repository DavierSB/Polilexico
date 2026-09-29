package classic

import (
	"time"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
)

// gameLog y turnRecord son el -log.json de cmd/vsbot, campo por campo, para poder revisar
// en la PC las partidas del telefono.
type gameLog struct {
	StartedAt   time.Time    `json:"started_at"`
	HumanName   string       `json:"human_name"`
	BotName     string       `json:"bot_name"`
	Turns       []turnRecord `json:"turns"`
	FinalScores [2]int       `json:"final_scores"`
	Winner      string       `json:"winner"`
	// Mode: "" en la clasica, ModeEndgame en Finales; StartTurn: en Finales, el primer turno que
	// jugaste tu (desde 0), donde empieza la revision. Campos que vsbot no tiene.
	Mode      string `json:"mode,omitempty"`
	StartTurn int    `json:"start_turn,omitempty"`
}

type turnRecord struct {
	TurnNum     int              `json:"turn_num"`
	PlayerName  string           `json:"player_name"`
	Rack        string           `json:"rack"`
	BoardBefore string           `json:"board_before"`
	GenList     []core.Candidate `json:"gen_list"`
	ActualPlay  core.Candidate   `json:"actual_play"`
	WasBest     bool             `json:"was_best"`
}

// Log: el registro de la partida en JSON, con las mejores jugadas de cada turno y la
// realizada, para la pantalla de revision.
func (c *Game) Log() (string, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	return core.JSON(c.log)
}

// newTurnRecord anota m antes de jugarla (el tablero y el turno son los de antes).
func (c *Game) newTurnRecord(m *move.Move, play core.Play, candidates []core.Candidate) turnRecord {
	r := turnRecordOf(c.g, m, play, candidates, len(c.log.Turns)+1)
	r.PlayerName = c.playerName(c.humanToMove())
	return r
}

func (c *Game) finishIfOver() {
	if c.over() {
		c.finish()
	}
}

// finish cierra el registro y lo escribe en disco.
func (c *Game) finish() {
	c.log.FinalScores = [2]int{c.score(true), c.score(false)}
	c.log.Winner = c.winnerName()
	c.recordPath = core.WriteRecord(c.recordPrefix(), c.log, c.g)
}

func (c *Game) recordPrefix() string {
	if c.log.Mode == ModeEndgame {
		return ModeEndgame
	}
	return "classic"
}

func (c *Game) playerName(human bool) string {
	if human {
		return humanName
	}
	return c.botName
}

func (c *Game) winnerName() string {
	switch c.outcome() {
	case core.OutcomeWin:
		return humanName
	case core.OutcomeLoss:
		return c.botName
	}
	return "empate"
}

// turnRecordOf anota m, del turno num, sin el nombre de quien la juega.
func turnRecordOf(g *game.Game, m *move.Move, play core.Play, candidates []core.Candidate, num int) turnRecord {
	actual := core.CandidateOf(play, float64(m.Equity()))
	return turnRecord{TurnNum: num, Rack: m.FullRack(), BoardBefore: core.RenderBoard(g), GenList: candidates,
		ActualPlay: actual, WasBest: isBest(actual, candidates)}
}

func isBest(actual core.Candidate, candidates []core.Candidate) bool {
	return len(candidates) > 0 && core.SameCandidate(actual, candidates[0])
}
