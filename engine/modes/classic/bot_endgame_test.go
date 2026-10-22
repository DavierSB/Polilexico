package classic

import (
	"testing"
	"time"
)

const endgameBudget = 3 * time.Second

func TestBotSearchesTheEndgameWithinItsBudget(t *testing.T) {
	c := findEndgame(t, 0, 0, -LeadLimit, LeadLimit)
	if _, err := c.Play("pasar"); err != nil {
		t.Fatal(err)
	}
	if c.endgameSnapshot() == nil {
		t.Fatal("con la bolsa vacía HastyBot debería buscar el final")
	}
	start := time.Now()
	if _, err := c.PlayBot(); err != nil {
		t.Fatal(err)
	}
	if spent := time.Since(start); spent > endgameBudget {
		t.Fatalf("el bot tardó %v", spent)
	}
}

func TestBotSkipsTheSearchWithTilesInTheBag(t *testing.T) {
	c := findEndgame(t, 2, 8, -LeadLimit, LeadLimit)
	if _, err := c.Play("pasar"); err != nil {
		t.Fatal(err)
	}
	if c.endgameSnapshot() != nil {
		t.Fatal("con fichas en la bolsa no hay final que buscar")
	}
}

func TestPelusaAlsoSearchesTheEndgame(t *testing.T) {
	c := findEndgame(t, 0, 0, -LeadLimit, LeadLimit)
	c.botName = "BetterBot"
	if c.endgameSnapshot() == nil {
		t.Fatal("Pelusa también debería buscar el final")
	}
}

func TestPelusaAlsoPlansThePreEndgame(t *testing.T) {
	c := findEndgame(t, 1, 7, -LeadLimit, LeadLimit)
	c.botName = "BetterBot"
	if c.preEndgameTurn() == nil {
		t.Fatal("Pelusa también debería jugar el pre-final")
	}
}
