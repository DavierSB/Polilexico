package main

import (
	"context"
	"log"
	"slices"
	"strings"
	"time"
)

const adjournedTTL = 72 * time.Hour

type adjourned struct {
	ID         string `json:"id"`
	PlayerA    string `json:"player_a"`
	PlayerB    string `json:"player_b"`
	NameA      string `json:"name_a"`
	NameB      string `json:"name_b"`
	CountryA   string `json:"country_a"`
	CountryB   string `json:"country_b"`
	TimeMs     int64  `json:"time_ms"`
	OvertimeMs int64  `json:"overtime_ms"`
	Void       bool   `json:"void"`
	Untimed    bool   `json:"untimed"`
	Save       string `json:"save"`
	CreatedAt  string `json:"created_at,omitempty"`
}

type adjournedItem struct {
	ID       string `json:"id"`
	Opponent string `json:"opponent"`
	Country  string `json:"country"`
	Online   bool   `json:"online,omitempty"`
}

func (a adjourned) fresh() bool {
	at, err := time.Parse(time.RFC3339, a.CreatedAt)
	return err != nil || time.Since(at) < adjournedTTL
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
	a.CreatedAt = time.Now().UTC().Format(time.RFC3339)
	h.mu.Lock()
	h.adjourned[a.ID] = a
	h.mu.Unlock()
	h.changed()
	go func() {
		ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		if err := h.store.SaveAdjourned(ctx, a); err != nil {
			log.Printf("guardar aplazada %s: %v", a.ID, err)
		}
	}()
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

func (h *hub) adjournedItems(playerID string) []adjournedItem {
	h.mu.Lock()
	defer h.mu.Unlock()
	items := []adjournedItem{}
	for _, a := range h.adjourned {
		if a.has(playerID) && a.fresh() {
			id, name, country := a.opponentOf(playerID)
			items = append(items, adjournedItem{ID: a.ID, Opponent: name, Country: country, Online: h.online[id] != nil})
		}
	}
	slices.SortFunc(items, func(x, y adjournedItem) int { return strings.Compare(x.ID, y.ID) })
	return items
}

func (h *hub) findAdjourned(id, playerID string) (adjourned, bool) {
	h.mu.Lock()
	defer h.mu.Unlock()
	a, ok := h.adjourned[id]
	return a, ok && a.has(playerID) && a.fresh()
}
