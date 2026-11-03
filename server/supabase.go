package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"time"
)

type supabaseStore struct {
	base   string
	key    string
	client *http.Client
}

type apiError struct {
	status int
	body   string
}

func (e *apiError) Error() string { return fmt.Sprintf("supabase %d: %s", e.status, e.body) }

type request struct {
	method string
	path   string
	body   any
	bearer string
	prefer string
}

func newSupabaseStore(base, key string) *supabaseStore {
	return &supabaseStore{base: base, key: key, client: &http.Client{Timeout: 15 * time.Second}}
}

func (s *supabaseStore) Name() string { return "Supabase " + s.base }

func (s *supabaseStore) call(ctx context.Context, r request, out any) error {
	req, err := s.newRequest(ctx, r)
	if err != nil {
		return err
	}
	resp, err := s.client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	data, _ := io.ReadAll(resp.Body)
	if resp.StatusCode >= 400 {
		return &apiError{resp.StatusCode, string(data)}
	}
	if out == nil || len(data) == 0 {
		return nil
	}
	return json.Unmarshal(data, out)
}

func (s *supabaseStore) newRequest(ctx context.Context, r request) (*http.Request, error) {
	var body io.Reader
	if r.body != nil {
		data, err := json.Marshal(r.body)
		if err != nil {
			return nil, err
		}
		body = bytes.NewReader(data)
	}
	req, err := http.NewRequestWithContext(ctx, r.method, s.base+r.path, body)
	if err != nil {
		return nil, err
	}
	req.Header.Set("apikey", s.key)
	req.Header.Set("Content-Type", "application/json")
	if r.bearer != "" {
		req.Header.Set("Authorization", "Bearer "+r.bearer)
	}
	if r.prefer != "" {
		req.Header.Set("Prefer", r.prefer)
	}
	return req, nil
}

func statusOf(err error) int {
	if e, ok := err.(*apiError); ok {
		return e.status
	}
	return 0
}

func eq(value string) string {
	return "eq." + url.QueryEscape(value)
}
