package main

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"sync"
	"time"
)

type memoryUser struct {
	player
	hash  string
	email string
}

type memoryStore struct {
	mu      sync.Mutex
	users   map[string]*memoryUser
	byName  map[string]string
	access  map[string]string
	refresh map[string]string
	results []result
	backups map[string]string
	photos  map[string]photo
	pending map[string]adjourned
}

func newMemoryStore() *memoryStore {
	return &memoryStore{users: map[string]*memoryUser{}, byName: map[string]string{}, access: map[string]string{},
		refresh: map[string]string{}, backups: map[string]string{}, photos: map[string]photo{}, pending: map[string]adjourned{}}
}

func (s *memoryStore) Name() string { return "sin Supabase: cuentas en memoria" }

func (s *memoryStore) SignUp(_ context.Context, a account) (session, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	key := memoryKey(a)
	if _, ok := s.byName[key]; ok {
		return session{}, errTaken
	}
	u := &memoryUser{player: player{ID: token(), Name: a.Name, Country: a.Country}, hash: hashOf(a.Password), email: a.Email}
	s.users[u.ID], s.byName[key] = u, u.ID
	return s.open(u), nil
}

func (s *memoryStore) LogIn(_ context.Context, a account) (session, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	u := s.users[s.byName[memoryKey(a)]]
	if u == nil || u.hash != hashOf(a.Password) {
		return session{}, errCredentials
	}
	return s.open(u), nil
}

func (s *memoryStore) Refresh(_ context.Context, refreshToken string) (session, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	u := s.users[s.refresh[refreshToken]]
	if u == nil {
		return session{}, errSession
	}
	delete(s.refresh, refreshToken)
	return s.open(u), nil
}

func (s *memoryStore) Identify(_ context.Context, accessToken string) (player, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	u := s.users[s.access[accessToken]]
	if u == nil {
		return player{}, errSession
	}
	return u.player, nil
}

func (s *memoryStore) Record(_ context.Context, r result) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.results = append(s.results, r)
	return nil
}

func (s *memoryStore) Stats(_ context.Context, p player) ([]statsRow, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	return tally(s.results, p.ID), nil
}

func (s *memoryStore) Backup(_ context.Context, p player) (string, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.backups[p.ID], nil
}

func (s *memoryStore) SaveBackup(_ context.Context, p player, data string) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.backups[p.ID] = data
	return nil
}

func (s *memoryStore) Ping(context.Context) error { return nil }

func (s *memoryStore) open(u *memoryUser) session {
	access, refresh := token(), token()
	s.access[access], s.refresh[refresh] = u.ID, u.ID
	return session{AccessToken: access, RefreshToken: refresh, ExpiresIn: 3600, Name: u.Name, Country: u.Country}
}

func memoryKey(a account) string {
	return nameKey(a.Name) + "/" + a.Country
}

func hashOf(password string) string {
	sum := sha256.Sum256([]byte(password))
	return hex.EncodeToString(sum[:])
}

func token() string {
	b := make([]byte, 16)
	rand.Read(b)
	return hex.EncodeToString(b)
}

func (s *memoryStore) SaveAdjourned(_ context.Context, a adjourned) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.pending[a.ID] = a
	return nil
}

func (s *memoryStore) Adjourned(_ context.Context, playerID string) ([]adjourned, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	var out []adjourned
	for _, a := range s.pending {
		if a.has(playerID) {
			out = append(out, a)
		}
	}
	return out, nil
}

func (s *memoryStore) DropAdjourned(_ context.Context, id string) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	delete(s.pending, id)
	return nil
}

func (s *memoryStore) DueAdjourned(_ context.Context, before time.Time) ([]adjourned, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	var out []adjourned
	for _, a := range s.pending {
		if a.pending() && a.createdAt().Before(before) {
			out = append(out, a)
		}
	}
	return out, nil
}
