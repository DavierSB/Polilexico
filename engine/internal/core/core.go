package core

import (
	"errors"
	"os"
	"strings"
	"sync"

	"github.com/rs/zerolog"

	"github.com/domino14/macondo/board"
	"github.com/domino14/macondo/config"
	"github.com/domino14/macondo/game"
	pb "github.com/domino14/macondo/gen/api/proto/macondo"
	"github.com/domino14/word-golib/tilemapping"
)

const Lexicon = "FILE2017"

const distribution = "spanish"

var ErrNotInitialized = errors.New("motor sin inicializar: llama a Init primero")

var (
	initMu   sync.Mutex
	cfg      *config.Config
	rules    *game.GameRules
	savesDir string
)

func Init(dataDir, saves string) error {
	initMu.Lock()
	defer initMu.Unlock()
	if rules != nil {
		return nil
	}
	zerolog.SetGlobalLevel(zerolog.ErrorLevel)
	return setUp(dataDir, saves)
}

func Ready() error {
	if rules == nil {
		return ErrNotInitialized
	}
	return nil
}

func NewGame(first, second string) (*game.Game, error) {
	if err := Ready(); err != nil {
		return nil, err
	}
	return game.NewGame(rules, players(first, second))
}

func NewGameFromBoard(rows [][]tilemapping.MachineLetter, rack string) (*game.Game, error) {
	if err := Ready(); err != nil {
		return nil, err
	}
	g, err := game.NewFromSnapshot(rules, players("tu", "rival"), []string{rack, ""}, []int{0, 0}, rows)
	if err != nil {
		return nil, err
	}
	g.RecalculateBoard()
	return g, nil
}

func RenamePlayers(g *game.Game, first, second string) error {
	if err := g.RenamePlayer(0, player(first)); err != nil {
		return err
	}
	return g.RenamePlayer(1, player(second))
}

func setUp(dataDir, saves string) error {
	c := newConfig(dataDir)
	r, err := newRules(c)
	if err != nil {
		return err
	}
	if err := os.MkdirAll(saves, 0o755); err != nil {
		return err
	}
	cfg, rules, savesDir = c, r, saves
	return nil
}

func newConfig(dataDir string) *config.Config {
	c := config.DefaultConfig()
	c.Set(config.ConfigDataPath, dataDir)
	c.Set(config.ConfigDefaultLexicon, Lexicon)
	c.Set(config.ConfigDefaultLetterDistribution, distribution)
	return c
}

func newRules(c *config.Config) (*game.GameRules, error) {
	return game.NewBasicGameRules(c, Lexicon, board.CrosswordGameLayout, distribution,
		game.CrossScoreAndSet, game.VarClassic)
}

func players(first, second string) []*pb.PlayerInfo {
	return []*pb.PlayerInfo{player(first), player(second)}
}

func player(name string) *pb.PlayerInfo {
	return &pb.PlayerInfo{Nickname: nickname(name), RealName: name}
}

func nickname(name string) string {
	return withoutAccents.Replace(strings.ToLower(name))
}

var withoutAccents = strings.NewReplacer("á", "a", "é", "e", "í", "i", "ó", "o", "ú", "u", "ñ", "n")
