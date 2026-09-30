package classic

import (
	"strings"
	"testing"

	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
	"lexico/engine/internal/testenv"
)

func TestFullGame(t *testing.T) {
	c := newTestGame(t)
	playTurns(t, c, 200)
	if !c.Status().Over || c.Result() == nil {
		t.Fatal("no terminó")
	}
	first := c.MoveAt(0)
	if first.Kind == core.KindPlay && first.Coords == "" {
		t.Fatalf("jugada sin coordenadas: %+v", first)
	}
	if c.OpponentRack() == "" && c.Status().OpponentTiles > 0 {
		t.Fatal("no reveló el atril del bot al terminar")
	}
	t.Logf("%+v | log: %d bytes", *c.Result(), len(mustLog(t, c)))
}

func TestQueriesAtStart(t *testing.T) {
	c := newTestGame(t)
	rack, unseen, status := strings.Fields(c.Rack()), strings.Fields(c.Unseen()), c.Status()
	if len(rack) != 7 || status.OpponentTiles != 7 || len(unseen) != status.BagCount+7 {
		t.Fatalf("atril %v, rival %d, sin ver %d, bolsa %d", rack, status.OpponentTiles, len(unseen), status.BagCount)
	}
	if c.OpponentRack() != "" {
		t.Fatal("reveló el atril del bot antes de terminar")
	}
	if strings.ContainsAny(c.Rack()+c.Unseen(), "[]") {
		t.Fatal("fichas con corchetes")
	}
}

func TestRejectsImpossiblePlacement(t *testing.T) {
	c := humanStartsWith(t, "TPNAEIO")
	if _, err := c.Play("h8 ZZZZ"); err == nil || !c.Status().HumanToMove {
		t.Fatal("aceptó fichas que no tienes")
	}
}

func TestInvalidPlayHidesWords(t *testing.T) {
	c := humanStartsWith(t, "TPNAEIO")
	_, err := c.Play("h8 TPN")
	if err == nil || err.Error() != "Jugada inválida" {
		t.Fatalf("debería decir solo \"Jugada inválida\": %v", err)
	}
	if !c.Status().HumanToMove {
		t.Fatal("sin SetInvalidPlayLosesTurn debería seguir siendo tu turno")
	}
}

func TestInvalidPlayLosesTurn(t *testing.T) {
	c := humanStartsWith(t, "TPNAEIO")
	c.SetInvalidPlayLosesTurn(true)
	played, err := c.Play("h8 TPN")
	if err != nil || played.Kind != core.KindInvalid || played.Coords != "H8" || played.HumanTotal != 0 {
		t.Fatalf("%+v %v", played, err)
	}
	if c.Status().HumanToMove {
		t.Fatal("no perdió el turno")
	}
}

func TestSaveAndLoad(t *testing.T) {
	c := newTestGame(t)
	c.SetInvalidPlayLosesTurn(true)
	playTurns(t, c, 7)
	loaded := mustReload(t, c)
	if c.Rack() != loaded.Rack() || c.Unseen() != loaded.Unseen() || *c.Status() != *loaded.Status() ||
		c.MoveCount() != loaded.MoveCount() || !loaded.invalidLosesTurn {
		t.Fatalf("no coincide:\n%+v\n%+v", *c.Status(), *loaded.Status())
	}
	playTurns(t, loaded, 200)
	if !loaded.Status().Over {
		t.Fatal("la partida recargada no llegó al final")
	}
}

func TestLoseOnTime(t *testing.T) {
	c := newTestGame(t)
	c.LoseOnTime()
	if r := c.Result(); r == nil || !r.LostOnTime || r.Outcome != core.OutcomeLoss {
		t.Fatalf("%+v", r)
	}
}

func TestEveryBot(t *testing.T) {
	testenv.Init(t)
	for _, name := range strings.Split(Bots(), ",") {
		checkBot(t, name)
	}
	if _, err := Start("BestBot"); err == nil {
		t.Fatal("BestBot no debería estar")
	}
}

func checkBot(t *testing.T, name string) {
	t.Helper()
	c, err := Start(name)
	if err != nil {
		t.Fatal(err)
	}
	playTurns(t, c, 200)
	if loaded := mustReload(t, c); loaded.Opponent() != name {
		t.Fatalf("%s recargado como %s", name, loaded.Opponent())
	}
	t.Logf("%-12s %d puntos", name, c.Status().BotScore)
}

func newTestGame(t *testing.T) *Game {
	t.Helper()
	testenv.Init(t)
	c, err := Start(core.DefaultBot)
	if err != nil {
		t.Fatal(err)
	}
	return c
}

func humanStartsWith(t *testing.T, rack string) *Game {
	t.Helper()
	c := newTestGame(t)
	for !c.HumanStarts() {
		c = newTestGame(t)
	}
	if err := c.g.SetRackFor(c.humanIdx, tilemapping.RackFromString(rack, c.g.Alphabet())); err != nil {
		t.Fatal(err)
	}
	return c
}

func playTurns(t *testing.T, c *Game, n int) {
	t.Helper()
	for i := 0; i < n && !c.Status().Over; i++ {
		if err := playTurn(c); err != nil {
			t.Fatal(err)
		}
	}
}

func playTurn(c *Game) error {
	if !c.Status().HumanToMove {
		_, err := c.PlayBot()
		return err
	}
	_, err := c.Play(testenv.Input(c.bot.GenerateMoves(1)[0]))
	return err
}

func mustReload(t *testing.T, c *Game) *Game {
	t.Helper()
	saved, err := c.Save()
	if err != nil {
		t.Fatal(err)
	}
	loaded, err := Load(saved)
	if err != nil {
		t.Fatal(err)
	}
	return loaded
}

func mustLog(t *testing.T, c *Game) string {
	t.Helper()
	log, err := c.Log()
	if err != nil {
		t.Fatal(err)
	}
	return log
}

func TestBoardHasTheTilesPlayed(t *testing.T) {
	c := humanStartsWith(t, "TPNAEIO")
	playTurns(t, c, 1)
	squares := strings.Fields(c.Board())
	placed := 0
	for _, s := range squares {
		if s != "." {
			placed++
		}
	}
	if len(squares) != 225 || placed != c.MoveAt(0).TileCount {
		t.Fatalf("%d casillas, %d fichas, jugada %+v", len(squares), placed, *c.MoveAt(0))
	}
}
