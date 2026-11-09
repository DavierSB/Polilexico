package main

import (
	"context"
	"net/http"
)

var errNoPlayer = userError("Ese jugador no existe.")

func profileStats(st store) playerHandler {
	return func(w http.ResponseWriter, r *http.Request, _ player) {
		q := r.URL.Query()
		p, err := st.Find(r.Context(), q.Get("name"), q.Get("country"))
		if err != nil {
			reply(w)(nil, err)
			return
		}
		rows, err := st.Stats(r.Context(), p)
		reply(w)(map[string]any{"name": p.Name, "country": p.Country, "rows": rows}, err)
	}
}

func (s *supabaseStore) Find(ctx context.Context, name, country string) (player, error) {
	prof, err := s.profileByName(ctx, account{Name: name, Country: country})
	if err != nil || prof == nil {
		return player{}, errorOr(err, errNoPlayer)
	}
	return player{ID: prof.ID, Name: prof.Username, Country: prof.Country}, nil
}

func (s *memoryStore) Find(_ context.Context, name, country string) (player, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	u := s.users[s.byName[memoryKey(account{Name: name, Country: country})]]
	if u == nil {
		return player{}, errNoPlayer
	}
	return u.player, nil
}
