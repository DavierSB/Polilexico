package testenv

import (
	"os"
	"path/filepath"
	"runtime"
	"sync"
	"testing"

	"github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
)

var (
	once    sync.Once
	initErr error
)

func Init(t testing.TB) {
	t.Helper()
	once.Do(func() { initErr = initEngine() })
	if initErr != nil {
		t.Fatal(initErr)
	}
}

func Input(m *move.Move) string {
	switch m.Action() {
	case move.MoveTypePass:
		return "pasar"
	case move.MoveTypeExchange:
		return "cambiar " + m.TilesStringExchange()
	}
	return core.MoveCoords(m) + " " + m.TilesString()
}

func initEngine() error {
	root, err := os.MkdirTemp("", "lexico-engine-test-")
	if err != nil {
		return err
	}
	data := filepath.Join(root, "data")
	if err := linkDataDir(data); err != nil {
		return err
	}
	return core.Init(data, filepath.Join(root, "saves"))
}

func linkDataDir(data string) error {
	links := map[string]string{
		"lexica/gaddag":       filepath.Join(thirdParty(), "woogles-lexica"),
		"letterdistributions": filepath.Join(thirdParty(), "macondo", "data", "letterdistributions"),
		"strategy":            filepath.Join(thirdParty(), "macondo", "data", "strategy"),
	}
	for name, target := range links {
		if err := link(filepath.Join(data, name), target); err != nil {
			return err
		}
	}
	return nil
}

func link(path, target string) error {
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return err
	}
	return os.Symlink(target, path)
}

func thirdParty() string {
	_, file, _, _ := runtime.Caller(0)
	return filepath.Join(filepath.Dir(file), "..", "..", "..", "third_party")
}
