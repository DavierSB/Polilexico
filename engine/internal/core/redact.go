package core

import (
	"encoding/json"

	"github.com/domino14/word-golib/tilemapping"
	"google.golang.org/protobuf/encoding/protojson"
	"google.golang.org/protobuf/proto"

	"github.com/domino14/macondo/game"
	pb "github.com/domino14/macondo/gen/api/proto/macondo"
)

func RedactedHistoryJSON(g *game.Game, viewer int) (json.RawMessage, error) {
	h := proto.Clone(g.History()).(*pb.GameHistory)
	if len(h.LastKnownRacks) == 2 {
		h.LastKnownRacks[1-viewer] = ""
	}
	for i, evt := range h.Events {
		if int(evt.PlayerIndex) != viewer {
			if err := redactEvent(h, i, evt); err != nil {
				return nil, err
			}
		}
	}
	return protojson.Marshal(h)
}

func redactEvent(h *pb.GameHistory, i int, evt *pb.GameEvent) error {
	switch evt.Type {
	case pb.GameEvent_TILE_PLACEMENT_MOVE:
		return keepPlayedTiles(evt)
	case pb.GameEvent_EXCHANGE:
		return standInExchange(h, i, evt)
	default:
		evt.Rack = ""
	}
	return nil
}

func keepPlayedTiles(evt *pb.GameEvent) error {
	alph := rules.LetterDistribution().TileMapping()
	played, err := tilemapping.ToMachineLetters(evt.PlayedTiles, alph)
	if err != nil {
		return err
	}
	var rack tilemapping.MachineWord
	for _, ml := range played {
		if ml.IsPlayedTile() {
			rack = append(rack, ml.IntrinsicTileIdx())
		}
	}
	evt.Rack = rack.UserVisible(alph)
	return nil
}

func standInExchange(h *pb.GameHistory, i int, evt *pb.GameEvent) error {
	exchanged, err := tilemapping.ToMachineLetters(evt.Exchanged, rules.LetterDistribution().TileMapping())
	if err != nil {
		return err
	}
	tiles, err := hiddenTiles(h, i, len(exchanged))
	if err != nil {
		return err
	}
	evt.Rack, evt.Exchanged = tiles, tiles
	return nil
}

func hiddenTiles(h *pb.GameHistory, turn, n int) (string, error) {
	g, err := game.NewFromHistory(proto.Clone(h).(*pb.GameHistory), rules, turn)
	if err != nil {
		return "", err
	}
	g.ThrowRacksIn()
	pool := tilemapping.MachineWord(g.Bag().Peek())
	return pool[:min(n, len(pool))].UserVisible(rules.LetterDistribution().TileMapping()), nil
}
