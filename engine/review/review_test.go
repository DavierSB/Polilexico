package review

import (
	"strconv"
	"strings"
	"testing"
)

const classicLog = `{"started_at":"2026-09-27T23:30:02","human_name":"Tú","bot_name":"HastyBot","final_scores":[421,485],"winner":"HastyBot",
"turns":[{"turn_num":1,"player_name":"Tú","rack":"AANNY[CH]?","gen_list":[{"coords":"H8","description":"H8 NANAY (24 pts)","score":24,"equity":20.5}],
"actual_play":{"coords":"H8","description":"H8 NANAY (24 pts)","score":24,"equity":20.5}},
{"turn_num":2,"player_name":"HastyBot","rack":"DEMNDAS","gen_list":[{"coords":"11E","description":"11E DEM.NDA (44 pts)","score":44,"equity":40}],
"actual_play":{"coords":"","description":"(Pasar)","score":0,"equity":0}}]}`

const duplicateLog = `{"started_at":"2026-09-27T23:30:02","final_master":994,"final_human":705,"final_aciertos":18,
"turns":[{"turn_num":1,"rack":"? A C D E G S","top_plays":[{"coords":"H8","description":"H8 [CH]Es (12 pts)","score":12,"equity":0}],
"master_play":{"coords":"H8","description":"H8 [CH]Es (12 pts)","score":12,"equity":0},
"human_play":{"coords":"H8","description":"H8 CAS (5 pts)","score":5,"equity":0}},
{"turn_num":2,"rack":"A B","top_plays":[],"master_play":{"description":"(Pasar)"},"human_play":{"description":"(Pasar)"}}]}`

func TestClassicLog(t *testing.T) {
	g := mustParse(t, classicLog)
	if g.Mode != ModeClassic || g.Opponent != "HastyBot" || g.Outcome != "loss" || g.TurnCount() != 2 {
		t.Fatalf("%+v", *g)
	}
	first, second := g.TurnAt(0), g.TurnAt(1)
	if first.Rack != "A A N N Y CH ?" || first.MarkAt(0).Rank != 0 || !first.CandidateAt(0).HasEquity {
		t.Fatalf("%+v %+v", *first, *first.MarkAt(0))
	}
	if g.BestWord != "NANAY" || g.BestWordScore != 24 || g.Bingos != 0 {
		t.Fatalf("palabras: %q %d %d", g.BestWord, g.BestWordScore, g.Bingos)
	}
	if squares(second.Board)["H12"] != "Y" || second.MarkAt(0).Rank != -1 {
		t.Fatalf("tablero del turno 2 o jugada fuera de lista: %+v", *second.MarkAt(0))
	}
}

func TestDuplicateLog(t *testing.T) {
	g := mustParse(t, duplicateLog)
	if g.Mode != ModeDuplicate || g.Outcome != "loss" || g.Hits != 18 || g.TurnAt(0).Player != "" {
		t.Fatalf("%+v", *g)
	}
	if g.BestWord != "CAS" || g.LongestWord != "CAS" {
		t.Fatalf("palabras: %q %q", g.BestWord, g.LongestWord)
	}
	marks := g.TurnAt(0).MarkCount()
	board := squares(g.TurnAt(1).Board)
	if marks != 2 || board["H8"] != "CH" || board["H10"] != "s" || g.TurnAt(0).MarkAt(1).Rank != -1 {
		t.Fatalf("marcas %d, tablero %v", marks, board)
	}
}

func mustParse(t *testing.T, text string) *Game {
	t.Helper()
	g, err := Parse(text)
	if err != nil {
		t.Fatal(err)
	}
	return g
}

func squares(board string) map[string]string {
	out := map[string]string{}
	for i, s := range strings.Fields(board) {
		if s != "." {
			out[string(rune('A'+i/size))+strconv.Itoa(i%size+1)] = s
		}
	}
	return out
}

func TestWordThroughBoardLettersAndBingo(t *testing.T) {
	board := newGrid()
	board.play("H8 CASA (12 pts)")
	if word, placed := board.word("8H .ERO (6 pts)"); word != "CERO" || placed != 3 {
		t.Fatalf("%q %d", word, placed)
	}
	if _, placed := board.word("I1 ABCDEFG (80 pts)"); placed != bingoTiles {
		t.Fatalf("scrabble: %d fichas", placed)
	}
}
