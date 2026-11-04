package main

import (
	"context"
)

const hiddenEmailDomain = "@cuentas.polilexico.app"

type profile struct {
	ID       string `json:"id"`
	Username string `json:"username"`
	Country  string `json:"country"`
}

type authUser struct {
	ID    string `json:"id"`
	Email string `json:"email"`
}

type tokens struct {
	AccessToken  string   `json:"access_token"`
	RefreshToken string   `json:"refresh_token"`
	ExpiresIn    int      `json:"expires_in"`
	User         authUser `json:"user"`
}

func (s *supabaseStore) SignUp(ctx context.Context, a account) (session, error) {
	if taken, err := s.taken(ctx, a); err != nil || taken {
		return session{}, errorOr(err, errTaken)
	}
	id, err := s.createUser(ctx, a)
	if err != nil {
		return session{}, err
	}
	if err := s.insertProfile(ctx, id, a); err != nil {
		s.call(ctx, request{method: "DELETE", path: "/auth/v1/admin/users/" + id}, nil)
		return session{}, conflictOr(err, errTaken)
	}
	return s.LogIn(ctx, a)
}

func (s *supabaseStore) LogIn(ctx context.Context, a account) (session, error) {
	p, err := s.profileByName(ctx, a)
	if err != nil || p == nil {
		return session{}, errorOr(err, errCredentials)
	}
	var u authUser
	if err := s.call(ctx, request{method: "GET", path: "/auth/v1/admin/users/" + p.ID}, &u); err != nil {
		return session{}, err
	}
	var t tokens
	body := map[string]string{"email": u.Email, "password": a.Password}
	if err := s.call(ctx, request{method: "POST", path: "/auth/v1/token?grant_type=password", body: body}, &t); err != nil {
		return session{}, conflictOr(err, errCredentials)
	}
	return sessionOf(t, *p), nil
}

func (s *supabaseStore) Refresh(ctx context.Context, refreshToken string) (session, error) {
	var t tokens
	body := map[string]string{"refresh_token": refreshToken}
	if err := s.call(ctx, request{method: "POST", path: "/auth/v1/token?grant_type=refresh_token", body: body}, &t); err != nil {
		return session{}, conflictOr(err, errSession)
	}
	p, err := s.profileByID(ctx, t.User.ID)
	if err != nil {
		return session{}, err
	}
	return sessionOf(t, p), nil
}

func (s *supabaseStore) Identify(ctx context.Context, accessToken string) (player, error) {
	var u authUser
	if err := s.call(ctx, request{method: "GET", path: "/auth/v1/user", bearer: accessToken}, &u); err != nil {
		return player{}, conflictOr(err, errSession)
	}
	p, err := s.profileByID(ctx, u.ID)
	return player{ID: p.ID, Name: p.Username, Country: p.Country}, err
}

func (s *supabaseStore) createUser(ctx context.Context, a account) (string, error) {
	email := a.Email
	if email == "" {
		email = token() + hiddenEmailDomain
	}
	var u authUser
	body := map[string]any{"email": email, "password": a.Password, "email_confirm": true}
	if err := s.call(ctx, request{method: "POST", path: "/auth/v1/admin/users", body: body}, &u); err != nil {
		return "", conflictOr(err, errEmailTaken)
	}
	return u.ID, nil
}

func (s *supabaseStore) insertProfile(ctx context.Context, id string, a account) error {
	body := map[string]string{"id": id, "username": a.Name, "country": a.Country}
	return s.call(ctx, request{method: "POST", path: "/rest/v1/profiles", body: body}, nil)
}

func (s *supabaseStore) taken(ctx context.Context, a account) (bool, error) {
	p, err := s.profileByName(ctx, a)
	return p != nil, err
}

func (s *supabaseStore) profileByName(ctx context.Context, a account) (*profile, error) {
	path := "/rest/v1/profiles?select=id,username,country&username_key=" + eq(nameKey(a.Name)) + "&country=" + eq(a.Country)
	var found []profile
	if err := s.call(ctx, request{method: "GET", path: path}, &found); err != nil || len(found) == 0 {
		return nil, err
	}
	return &found[0], nil
}

func (s *supabaseStore) profileByID(ctx context.Context, id string) (profile, error) {
	var found []profile
	err := s.call(ctx, request{method: "GET", path: "/rest/v1/profiles?select=id,username,country&id=" + eq(id)}, &found)
	if err == nil && len(found) == 0 {
		err = errSession
	}
	if err != nil {
		return profile{}, err
	}
	return found[0], nil
}

func sessionOf(t tokens, p profile) session {
	return session{AccessToken: t.AccessToken, RefreshToken: t.RefreshToken, ExpiresIn: t.ExpiresIn, Name: p.Username, Country: p.Country}
}

func conflictOr(err error, client userError) error {
	if s := statusOf(err); s >= 400 && s < 500 {
		return client
	}
	return err
}

func errorOr(err error, fallback userError) error {
	if err != nil {
		return err
	}
	return fallback
}
