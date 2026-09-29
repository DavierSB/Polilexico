package core

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/gcgio"
)

// WriteRecord guarda una partida terminada en la carpeta de Init: el registro log como
// <prefix>-<fecha>-log.json y la partida como .gcg. Devuelve la ruta del -log.json, o ""
// si no se pudo escribir (el registro es opcional: no detiene la partida).
func WriteRecord(prefix string, log any, g *game.Game) string {
	base := recordBase(prefix)
	if err := writeJSON(base+"-log.json", log); err != nil {
		return ""
	}
	writeGCG(base+".gcg", g)
	return base + "-log.json"
}

func recordBase(prefix string) string {
	stamp := time.Now().Format("20060102-150405")
	return filepath.Join(savesDir, fmt.Sprintf("%s-%s", prefix, stamp))
}

func writeJSON(path string, v any) error {
	data, err := json.MarshalIndent(v, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(path, data, 0o644)
}

func writeGCG(path string, g *game.Game) {
	if gcg, err := gcgio.GameHistoryToGCG(g.History(), true); err == nil {
		_ = os.WriteFile(path, []byte(gcg), 0o644)
	}
}
