package main

import (
	"context"
	"log"
	"slices"
	"strings"
	"time"
)

const (
	adjournedTTL  = 72 * time.Hour
	sweepEvery    = 10 * time.Minute
	resumeTimeout = 2 * time.Minute
	outcomeA      = "a"
	outcomeB      = "b"
	outcomeNone   = "none"
)

type adjourned struct {
	ID          string `json:"id"`
	Mode        string `json:"mode"`
	PlayerA     string `json:"player_a"`
	PlayerB     string `json:"player_b"`
	NameA       string `json:"name_a"`
	NameB       string `json:"name_b"`
	CountryA    string `json:"country_a"`
	CountryB    string `json:"country_b"`
	TimeMs      int64  `json:"time_ms"`
	OvertimeMs  int64  `json:"overtime_ms"`
	Void        bool   `json:"void"`
	Untimed     bool   `json:"untimed"`
	Save        string `json:"save"`
	Responsible string `json:"responsible"`
	AfterPlay   bool   `json:"after_play"`
	Outcome     string `json:"outcome"`
	SeenA       bool   `json:"seen_a"`
	SeenB       bool   `json:"seen_b"`
	CreatedAt   string `json:"created_at,omitempty"`
}

type adjournedItem struct {
	ID       string `json:"id"`
	Opponent string `json:"opponent"`
	Country  string `json:"country"`
	Online   bool   `json:"online,omitempty"`
	Busy     bool   `json:"busy,omitempty"`
	Result    string `json:"result,omitempty"`
	Duplicate bool   `json:"duplicate,omitempty"`
}

func (a adjourned) createdAt() time.Time {
	at, err := time.Parse(time.RFC3339, a.CreatedAt)
	if err != nil {
		return time.Now()
	}
	return at
}

func (a adjourned) pending() bool {
	return a.Outcome == ""
}

func (a adjourned) has(playerID string) bool {
	return a.PlayerA == playerID || a.PlayerB == playerID
}

func (a adjourned) opponentOf(playerID string) (id, name, country string) {
	if a.PlayerA == playerID {
		return a.PlayerB, a.NameB, a.CountryB
	}
	return a.PlayerA, a.NameA, a.CountryA
}

func (a adjourned) seenBy(playerID string) bool {
	if a.PlayerA == playerID {
		return a.SeenA
	}
	return a.SeenB
}

func (a adjourned) resultFor(playerID string) string {
	switch {
	case a.Outcome == outcomeNone:
		return "none"
	case (a.Outcome == outcomeA) == (a.PlayerA == playerID):
		return "win"
	}
	return "loss"
}

func (h *hub) loadAdjourned(playerID string) {
	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
	defer cancel()
	list, err := h.store.Adjourned(ctx, playerID)
	if err != nil {
		log.Printf("aplazadas de %s: %v", playerID, err)
		return
	}
	h.mu.Lock()
	for _, a := range list {
		h.adjourned[a.ID] = a
	}
	h.mu.Unlock()
	h.changed()
}

func (h *hub) keepAdjourned(a adjourned) {
	if a.CreatedAt == "" {
		a.CreatedAt = time.Now().UTC().Format(time.RFC3339)
	}
	h.mu.Lock()
	h.adjourned[a.ID] = a
	h.mu.Unlock()
	h.changed()
	go h.persist(a)
}

func (h *hub) persist(a adjourned) {
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	if err := h.store.SaveAdjourned(ctx, a); err != nil {
		log.Printf("guardar aplazada %s: %v", a.ID, err)
	}
}

func (h *hub) forgetAdjourned(id string) {
	h.mu.Lock()
	delete(h.adjourned, id)
	h.mu.Unlock()
	go func() {
		ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		if err := h.store.DropAdjourned(ctx, id); err != nil {
			log.Printf("borrar aplazada %s: %v", id, err)
		}
	}()
}

func (h *hub) blame(id, playerID string) {
	h.mu.Lock()
	a, ok := h.adjourned[id]
	h.mu.Unlock()
	if ok && a.pending() {
		a.Responsible, a.AfterPlay = playerID, false
		h.keepAdjourned(a)
	}
}

func (h *hub) markSeen(c *client, id string) {
	h.mu.Lock()
	a, ok := h.adjourned[id]
	h.mu.Unlock()
	if !ok || a.pending() || !a.has(c.player.ID) {
		return
	}
	if a.PlayerA == c.player.ID {
		a.SeenA = true
	} else {
		a.SeenB = true
	}
	if a.SeenA && a.SeenB {
		h.forgetAdjourned(id)
		h.changed()
		return
	}
	h.keepAdjourned(a)
}

func (h *hub) adjournedItems(playerID string) []adjournedItem {
	h.mu.Lock()
	defer h.mu.Unlock()
	items := []adjournedItem{}
	for _, a := range h.adjourned {
		if a.has(playerID) && (a.pending() || !a.seenBy(playerID)) {
			items = append(items, h.itemFor(a, playerID))
		}
	}
	slices.SortFunc(items, func(x, y adjournedItem) int { return strings.Compare(x.ID, y.ID) })
	return items
}

func (h *hub) itemFor(a adjourned, playerID string) adjournedItem {
	id, name, country := a.opponentOf(playerID)
	item := adjournedItem{ID: a.ID, Opponent: name, Country: country, Online: h.online[id] != nil, Busy: h.seats[id] != nil,
		Duplicate: a.Mode == modeDuplicate}
	if !a.pending() {
		item.Result = a.resultFor(playerID)
	}
	return item
}

func (h *hub) findAdjourned(id, playerID string) (adjourned, bool) {
	h.mu.Lock()
	defer h.mu.Unlock()
	a, ok := h.adjourned[id]
	return a, ok && a.has(playerID) && a.pending()
}

func (h *hub) sweepForever() {
	for {
		h.sweep(time.Now())
		time.Sleep(sweepEvery)
	}
}

func (h *hub) sweep(now time.Time) {
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	due, err := h.store.DueAdjourned(ctx, now.Add(-adjournedTTL))
	if err != nil {
		log.Printf("aplazadas vencidas: %v", err)
		return
	}
	for _, a := range due {
		h.settle(h.current(a))
	}
}

func (h *hub) current(a adjourned) adjourned {
	h.mu.Lock()
	defer h.mu.Unlock()
	if cached, ok := h.adjourned[a.ID]; ok {
		return cached
	}
	return a
}

func (h *hub) settle(a adjourned) {
	if !a.pending() {
		return
	}
	a.Outcome = adjudicateSaved(h.store, a)
	h.keepAdjourned(a)
}
