package duplicate

import (
	"time"

	"github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
)

// Jugadas del máster que se guardan por turno para la revision.
const reviewTopPlays = 15

// gameLog y turnRecord son el -log.json de cmd/duplicate, campo por campo, para poder revisar
// en la PC las partidas del telefono.
type gameLog struct {
	StartedAt   time.Time    `json:"started_at"`
	Turns       []turnRecord `json:"turns"`
	FinalMaster int          `json:"final_master"`
	FinalHuman  int          `json:"final_human"`
	FinalHits   int          `json:"final_aciertos"`
}

type turnRecord struct {
	TurnNum     int              `json:"turn_num"`
	Rack        string           `json:"rack"`
	BoardBefore string           `json:"board_before"`
	TopPlays    []core.Candidate `json:"top_plays"`
	MasterPlay  core.Candidate   `json:"master_play"`
	HumanPlay   core.Candidate   `json:"human_play"`
	HumanStatus string           `json:"human_status"`
	HumanScore  int              `json:"human_score"`
	Hit         bool             `json:"acierto"`
	MasterTotal int              `json:"master_total"`
	HumanTotal  int              `json:"human_total"`
	Hits        int              `json:"aciertos"`
}

// Log: el registro de la partida en JSON, con las mejores jugadas de cada turno, la del
// máster y la tuya, para la pantalla de revision.
func (d *Game) Log() (string, error) {
	d.mu.Lock()
	defer d.mu.Unlock()
	return core.JSON(d.log)
}

// newTurnRecord anota el turno t; se llama antes de soltar las jugadas del turno.
func (d *Game) newTurnRecord(t *Turn, human core.Play) turnRecord {
	top := core.Candidates(d.topPlays())
	return turnRecord{TurnNum: t.Number, Rack: t.Rack, BoardBefore: d.boardBefore, TopPlays: top,
		MasterPlay: top[0], HumanPlay: core.CandidateOf(human, 0), HumanStatus: human.Kind,
		HumanScore: t.HumanScore, Hit: t.Hit, MasterTotal: t.MasterTotal, HumanTotal: t.HumanTotal,
		Hits: t.Hits}
}

func (d *Game) finishIfOver() {
	if d.over() {
		d.finish()
	}
}

// finish cierra el registro y lo escribe en disco.
func (d *Game) finish() {
	d.log.FinalMaster, d.log.FinalHuman, d.log.FinalHits = d.masterTotal, d.humanTotal, d.hits
	d.recordPath = core.WriteRecord("duplicate", d.log, d.g)
}

func (d *Game) topPlays() []*move.Move {
	if len(d.plays) > reviewTopPlays {
		return d.plays[:reviewTopPlays]
	}
	return d.plays
}
