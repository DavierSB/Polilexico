package main

import (
	"encoding/json"
	"strings"

	"lexico/engine"
)

type candidate struct {
	Description string `json:"description"`
	Score       int    `json:"score"`
}

func engineRack(spaced string) string {
	var b strings.Builder
	for _, t := range strings.Fields(spaced) {
		if len([]rune(t)) > 1 {
			b.WriteString("[" + t + "]")
		} else {
			b.WriteString(t)
		}
	}
	return b.String()
}

func best(board, rack string, n int) []candidate {
	text, err := engine.BestMoves(board, engineRack(rack), n)
	if err != nil {
		logf("motor", "BestMoves: %v", err)
		return nil
	}
	var out []candidate
	json.Unmarshal([]byte(text), &out)
	return out
}

func input(c candidate) string {
	d := c.Description
	if i := strings.Index(d, " ("); i > 0 {
		return d[:i]
	}
	return "pasar"
}

func pick(board, rack string, rank int) string {
	cs := best(board, rack, rank+1)
	if len(cs) == 0 {
		return "pasar"
	}
	return input(cs[min(rank, len(cs)-1)])
}

func reversedWord(in string) string {
	f := strings.Fields(in)
	if len(f) != 2 {
		return in
	}
	var toks []string
	for _, r := range f[1] {
		toks = append(toks, string(r))
	}
	var joined []string
	for i := 0; i < len(toks); i++ {
		if toks[i] == "[" {
			j := i
			for toks[j] != "]" {
				j++
			}
			joined = append(joined, strings.Join(toks[i:j+1], ""))
			i = j
		} else {
			joined = append(joined, toks[i])
		}
	}
	for i, j := 0, len(joined)-1; i < j; i, j = i+1, j-1 {
		joined[i], joined[j] = joined[j], joined[i]
	}
	return f[0] + " " + strings.Join(joined, "")
}
