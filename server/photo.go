package main

import (
	"bytes"
	"context"
	"encoding/base64"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"
)

const (
	maxPhotoBytes   = 160 << 10
	maxPhotoRequest = 240 << 10
)

const (
	errPhoto   = userError("La foto no es válida.")
	errNoPhoto = userError("Ese jugador no tiene foto.")
)

var jpegMagic = []byte{0xFF, 0xD8, 0xFF}

type photo struct {
	Data    string `json:"data"`
	Version string `json:"updated_at"`
}

func checkPhoto(data string) error {
	if data == "" {
		return nil
	}
	raw, err := base64.StdEncoding.DecodeString(data)
	if err != nil || len(raw) > maxPhotoBytes || !bytes.HasPrefix(raw, jpegMagic) {
		return errPhoto
	}
	return nil
}

type photoCache struct {
	mu    sync.Mutex
	items map[string]cachedPhoto
}

type cachedPhoto struct {
	photo
	at time.Time
}

const photoCacheTTL = 30 * time.Minute

func newPhotoCache() *photoCache {
	return &photoCache{items: map[string]cachedPhoto{}}
}

func photoKey(name, country string) string {
	return nameKey(name) + "/" + strings.ToUpper(country)
}

func (c *photoCache) get(ctx context.Context, st store, name, country string) (photo, error) {
	return c.fetch(photoKey(name, country), func() (photo, error) { return st.Photo(ctx, name, country) })
}

func (c *photoCache) byID(ctx context.Context, st store, id string) (photo, error) {
	if !validID(id) {
		return photo{}, errNoPhoto
	}
	return c.fetch(idKey(id), func() (photo, error) { return st.PhotoByID(ctx, id) })
}

func (c *photoCache) fetch(key string, load func() (photo, error)) (photo, error) {
	c.mu.Lock()
	hit, ok := c.items[key]
	c.mu.Unlock()
	if ok && time.Since(hit.at) < photoCacheTTL {
		return hit.photo, nil
	}
	ph, err := load()
	if err == nil {
		c.put(key, ph)
	}
	return ph, err
}

func idKey(id string) string {
	return "#" + id
}

const maxIDLength = 64

func validID(id string) bool {
	if id == "" || len(id) > maxIDLength {
		return false
	}
	return strings.Trim(id, "0123456789abcdefABCDEF-") == ""
}

func (c *photoCache) put(key string, ph photo) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.items[key] = cachedPhoto{ph, time.Now()}
}

func (c *photoCache) forget(p player) {
	c.mu.Lock()
	defer c.mu.Unlock()
	delete(c.items, photoKey(p.Name, p.Country))
	delete(c.items, idKey(p.ID))
}

func savePhoto(st store, cache *photoCache) playerHandler {
	return func(w http.ResponseWriter, r *http.Request, p player) {
		r.Body = http.MaxBytesReader(w, r.Body, maxPhotoRequest)
		var body struct{ Data string }
		if !decode(w, r, &body) {
			return
		}
		if err := checkPhoto(body.Data); err != nil {
			reply(w)(nil, err)
			return
		}
		err := st.SavePhoto(r.Context(), p, body.Data)
		cache.forget(p)
		reply(w)(map[string]bool{"ok": true}, err)
	}
}

func servePhoto(st store, cache *photoCache) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ph, err := lookupPhoto(r.Context(), st, cache, r.URL.Query())
		if err != nil || ph.Data == "" {
			http.NotFound(w, r)
			return
		}
		writePhoto(w, r, ph)
	}
}

func lookupPhoto(ctx context.Context, st store, cache *photoCache, q url.Values) (photo, error) {
	if id := q.Get("id"); id != "" {
		return cache.byID(ctx, st, id)
	}
	return cache.get(ctx, st, q.Get("name"), q.Get("country"))
}

func writePhoto(w http.ResponseWriter, r *http.Request, ph photo) {
	tag := `"` + ph.Version + `"`
	w.Header().Set("ETag", tag)
	w.Header().Set("Cache-Control", "no-cache")
	if r.Header.Get("If-None-Match") == tag {
		w.WriteHeader(http.StatusNotModified)
		return
	}
	raw, _ := base64.StdEncoding.DecodeString(ph.Data)
	w.Header().Set("Content-Type", "image/jpeg")
	w.Write(raw)
}

func photoVersion() string {
	return time.Now().UTC().Format(time.RFC3339Nano)
}

func (s *memoryStore) SavePhoto(_ context.Context, p player, data string) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if data == "" {
		delete(s.photos, p.ID)
		return nil
	}
	s.photos[p.ID] = photo{Data: data, Version: photoVersion()}
	return nil
}

func (s *memoryStore) Photo(_ context.Context, name, country string) (photo, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	id, ok := s.byName[memoryKey(account{Name: name, Country: country})]
	if !ok {
		return photo{}, errNoPhoto
	}
	return s.photos[id], nil
}

func (s *memoryStore) PhotoByID(_ context.Context, id string) (photo, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.photos[id], nil
}

func (s *supabaseStore) SavePhoto(ctx context.Context, p player, data string) error {
	if data == "" {
		return s.call(ctx, request{method: "DELETE", path: "/rest/v1/photos?user_id=" + eq(p.ID)}, nil)
	}
	body := map[string]string{"user_id": p.ID, "data": data, "updated_at": photoVersion()}
	return s.call(ctx, request{method: "POST", path: "/rest/v1/photos", body: body, prefer: "resolution=merge-duplicates"}, nil)
}

func (s *supabaseStore) Photo(ctx context.Context, name, country string) (photo, error) {
	prof, err := s.profileByName(ctx, account{Name: name, Country: country})
	if err != nil || prof == nil {
		return photo{}, errorOr(err, errNoPhoto)
	}
	return s.PhotoByID(ctx, prof.ID)
}

func (s *supabaseStore) PhotoByID(ctx context.Context, id string) (photo, error) {
	var rows []photo
	err := s.call(ctx, request{method: "GET", path: "/rest/v1/photos?select=data,updated_at&user_id=" + eq(id)}, &rows)
	if err != nil || len(rows) == 0 {
		return photo{}, err
	}
	return rows[0], nil
}
