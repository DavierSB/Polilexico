package main

import (
	"encoding/json"
	"log"
	"math/rand/v2"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"unicode/utf8"

	"lexico/engine"
)

type candidate struct {
	Description string `json:"description"`
}

func initEngine(thirdParty string) {
	root, _ := os.MkdirTemp("", "onlinebot-")
	data := filepath.Join(root, "data")
	links := map[string]string{
		"lexica/gaddag":       filepath.Join(thirdParty, "woogles-lexica"),
		"letterdistributions": filepath.Join(thirdParty, "macondo", "data", "letterdistributions"),
		"strategy":            filepath.Join(thirdParty, "macondo", "data", "strategy"),
	}
	for name, target := range links {
		p := filepath.Join(data, name)
		os.MkdirAll(filepath.Dir(p), 0o755)
		abs, _ := filepath.Abs(target)
		os.Symlink(abs, p)
	}
	if err := engine.Init(data, filepath.Join(root, "saves")); err != nil {
		log.Fatal(err)
	}
}

func pick(board, rack string) string {
	return pickAmong(board, rack, 1)
}

func pickRandom(board, rack string) string {
	return pickAmong(board, rack, 20)
}

func pickAmong(board, rack string, n int) string {
	text, err := engine.BestMoves(board, engineRack(rack), n)
	if err != nil {
		log.Printf("motor: %v", err)
		return "pasar"
	}
	var cs []candidate
	json.Unmarshal([]byte(text), &cs)
	if len(cs) == 0 {
		return "pasar"
	}
	return input(cs[rand.IntN(len(cs))])
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

func input(c candidate) string {
	d := c.Description
	if tiles, ok := strings.CutPrefix(d, "(Cambiar "); ok {
		return "cambiar " + strings.TrimSuffix(tiles, ")")
	}
	if i := strings.Index(d, " ("); i > 0 {
		return d[:i]
	}
	return "pasar"
}

func itoa(n int) string {
	return strconv.Itoa(n)
}

func reversedWord(in string) string {
	f := strings.Fields(in)
	if len(f) != 2 || strings.Contains(f[1], ".") {
		return in
	}
	tiles := splitTiles(f[1])
	for i, j := 0, len(tiles)-1; i < j; i, j = i+1, j-1 {
		tiles[i], tiles[j] = tiles[j], tiles[i]
	}
	return f[0] + " " + strings.Join(tiles, "")
}

func splitTiles(word string) []string {
	var tiles []string
	for i := 0; i < len(word); {
		end := i + 1
		if word[i] == '[' {
			end = strings.IndexByte(word[i:], ']') + i + 1
		} else {
			_, size := utf8.DecodeRuneInString(word[i:])
			end = i + size
		}
		tiles = append(tiles, word[i:end])
		i = end
	}
	return tiles
}
