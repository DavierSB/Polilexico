package core

import (
	"encoding/json"

	"google.golang.org/protobuf/encoding/protojson"

	"github.com/domino14/macondo/game"
	pb "github.com/domino14/macondo/gen/api/proto/macondo"
)

// Para guardar y continuar partidas, la partida en si es el historial de macondo
// (pb.GameHistory, el mismo que el shell carga con `load`).

// HistoryJSON: el historial de g, para guardarlo.
func HistoryJSON(g *game.Game) (json.RawMessage, error) {
	return protojson.Marshal(g.History())
}

// GameFromHistory reconstruye una partida guardada con HistoryJSON. racks, si no es nil,
// reemplaza los ultimos atriles conocidos.
func GameFromHistory(raw json.RawMessage, racks []string) (*game.Game, error) {
	if err := Ready(); err != nil {
		return nil, err
	}
	h := &pb.GameHistory{}
	if err := protojson.Unmarshal(raw, h); err != nil {
		return nil, err
	}
	if racks != nil {
		h.LastKnownRacks = racks
	}
	return game.NewFromHistory(h, rules, len(h.Events))
}

// JSON: v como texto JSON, para devolverlo a Android.
func JSON(v any) (string, error) {
	b, err := json.Marshal(v)
	return string(b), err
}
