package main

import (
	"context"
	"net/http"
	"strings"
)

type accountView struct {
	Name    string `json:"name"`
	Country string `json:"country"`
	Email   string `json:"email"`
}

func accountInfo(st store) playerHandler {
	return func(w http.ResponseWriter, r *http.Request, p player) {
		reply(w)(st.Account(r.Context(), p))
	}
}

func updateAccount(st store, photos *photoCache) playerHandler {
	return func(w http.ResponseWriter, r *http.Request, p player) {
		var a account
		if !decode(w, r, &a) {
			return
		}
		a = a.normalized()
		if err := a.checkEdit(); err != nil {
			reply(w)(nil, err)
			return
		}
		photos.forget(p)
		reply(w)(st.UpdateAccount(r.Context(), p, a))
	}
}

func (a account) checkEdit() error {
	if a.Password != "" && len(a.Password) < 6 {
		return errBadPassword
	}
	return a.check(false)
}

func (a account) renames(p player) bool {
	return nameKey(a.Name) != nameKey(p.Name) || a.Country != p.Country
}

func visibleEmail(email string) string {
	if strings.HasSuffix(email, hiddenEmailDomain) {
		return ""
	}
	return email
}

func (s *memoryStore) Account(_ context.Context, p player) (accountView, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	u := s.users[p.ID]
	if u == nil {
		return accountView{}, errSession
	}
	return accountView{u.Name, u.Country, u.email}, nil
}

func (s *memoryStore) UpdateAccount(_ context.Context, p player, a account) (accountView, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	u := s.users[p.ID]
	if u == nil {
		return accountView{}, errSession
	}
	if id, ok := s.byName[memoryKey(a)]; ok && id != p.ID {
		return accountView{}, errTaken
	}
	delete(s.byName, memoryKey(account{Name: u.Name, Country: u.Country}))
	u.Name, u.Country, u.email = a.Name, a.Country, a.Email
	s.byName[memoryKey(a)] = u.ID
	if a.Password != "" {
		u.hash = hashOf(a.Password)
	}
	return accountView{u.Name, u.Country, u.email}, nil
}

func (s *supabaseStore) Account(ctx context.Context, p player) (accountView, error) {
	var u authUser
	if err := s.call(ctx, request{method: "GET", path: "/auth/v1/admin/users/" + p.ID}, &u); err != nil {
		return accountView{}, err
	}
	return accountView{p.Name, p.Country, visibleEmail(u.Email)}, nil
}

func (s *supabaseStore) UpdateAccount(ctx context.Context, p player, a account) (accountView, error) {
	if a.renames(p) {
		if err := s.rename(ctx, p, a); err != nil {
			return accountView{}, err
		}
	}
	if err := s.updateAuth(ctx, p, a); err != nil {
		return accountView{}, err
	}
	return accountView{a.Name, a.Country, a.Email}, nil
}

func (s *supabaseStore) rename(ctx context.Context, p player, a account) error {
	if found, err := s.profileByName(ctx, a); err != nil || (found != nil && found.ID != p.ID) {
		return errorOr(err, errTaken)
	}
	body := map[string]string{"username": a.Name, "country": a.Country}
	return conflictOr(s.call(ctx, request{method: "PATCH", path: "/rest/v1/profiles?id=" + eq(p.ID), body: body}, nil), errTaken)
}

func (s *supabaseStore) updateAuth(ctx context.Context, p player, a account) error {
	current, err := s.Account(ctx, p)
	if err != nil {
		return err
	}
	body := map[string]any{}
	if a.Password != "" {
		body["password"] = a.Password
	}
	if a.Email != current.Email {
		body["email"], body["email_confirm"] = emailOrHidden(a.Email), true
	}
	if len(body) == 0 {
		return nil
	}
	return conflictOr(s.call(ctx, request{method: "PUT", path: "/auth/v1/admin/users/" + p.ID, body: body}, nil), errEmailTaken)
}

func emailOrHidden(email string) string {
	if email == "" {
		return token() + hiddenEmailDomain
	}
	return email
}
