package main

import (
	"context"
	"net/url"
	"time"
)

type gameRow struct {
	ID int64 `json:"id"`
}

type playerRow struct {
	placing
	GameID int64 `json:"game_id"`
}

type backupRow struct {
	UserID    string `json:"user_id,omitempty"`
	Data      string `json:"data"`
	UpdatedAt string `json:"updated_at,omitempty"`
}

func (s *supabaseStore) Record(ctx context.Context, r result) error {
	var games []gameRow
	body := map[string]any{"mode": r.Mode, "settings": r.Settings, "started_at": r.StartedAt}
	if err := s.call(ctx, request{method: "POST", path: "/rest/v1/games", body: body, prefer: "return=representation"}, &games); err != nil {
		return err
	}
	rows := make([]playerRow, len(r.Players))
	for i, p := range r.Players {
		rows[i] = playerRow{placing: p, GameID: games[0].ID}
	}
	return s.call(ctx, request{method: "POST", path: "/rest/v1/game_players", body: rows}, nil)
}

func (s *supabaseStore) Stats(ctx context.Context, p player) ([]statsRow, error) {
	var rows []statsRow
	err := s.call(ctx, request{method: "GET", path: "/rest/v1/player_stats?user_id=" + eq(p.ID)}, &rows)
	return rows, err
}

func (s *supabaseStore) Backup(ctx context.Context, p player) (string, error) {
	var rows []backupRow
	if err := s.call(ctx, request{method: "GET", path: "/rest/v1/backups?select=data&user_id=" + eq(p.ID)}, &rows); err != nil || len(rows) == 0 {
		return "", err
	}
	return rows[0].Data, nil
}

func (s *supabaseStore) SaveBackup(ctx context.Context, p player, data string) error {
	body := backupRow{UserID: p.ID, Data: data, UpdatedAt: time.Now().UTC().Format(time.RFC3339)}
	return s.call(ctx, request{method: "POST", path: "/rest/v1/backups", body: body, prefer: "resolution=merge-duplicates"}, nil)
}

func (s *supabaseStore) Ping(ctx context.Context) error {
	return s.call(ctx, request{method: "GET", path: "/rest/v1/profiles?select=id&limit=1"}, nil)
}

func (s *supabaseStore) SaveAdjourned(ctx context.Context, a adjourned) error {
	return s.call(ctx, request{method: "POST", path: "/rest/v1/adjourned_games", body: a, prefer: "resolution=merge-duplicates"}, nil)
}

func (s *supabaseStore) Adjourned(ctx context.Context, playerID string) ([]adjourned, error) {
	path := "/rest/v1/adjourned_games?select=*&or=(player_a." + eq(playerID) + ",player_b." + eq(playerID) + ")"
	var rows []adjourned
	err := s.call(ctx, request{method: "GET", path: path}, &rows)
	return rows, err
}

func (s *supabaseStore) DropAdjourned(ctx context.Context, id string) error {
	return s.call(ctx, request{method: "DELETE", path: "/rest/v1/adjourned_games?id=" + eq(id)}, nil)
}

func (s *supabaseStore) DueAdjourned(ctx context.Context, before time.Time) ([]adjourned, error) {
	path := "/rest/v1/adjourned_games?select=*&outcome=eq.&created_at=lt." + url.QueryEscape(before.UTC().Format(time.RFC3339))
	var rows []adjourned
	err := s.call(ctx, request{method: "GET", path: path}, &rows)
	return rows, err
}
