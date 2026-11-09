package main

import (
	"context"
	"errors"
	"regexp"
	"strings"
	"time"
)

type store interface {
	Name() string
	SignUp(ctx context.Context, a account) (session, error)
	LogIn(ctx context.Context, a account) (session, error)
	Refresh(ctx context.Context, refreshToken string) (session, error)
	Identify(ctx context.Context, accessToken string) (player, error)
	Record(ctx context.Context, r result) error
	Stats(ctx context.Context, p player) ([]statsRow, error)
	Find(ctx context.Context, name, country string) (player, error)
	Backup(ctx context.Context, p player) (string, error)
	SaveBackup(ctx context.Context, p player, data string) error
	SavePhoto(ctx context.Context, p player, data string) error
	Account(ctx context.Context, p player) (accountView, error)
	UpdateAccount(ctx context.Context, p player, a account) (accountView, error)
	Photo(ctx context.Context, name, country string) (photo, error)
	Ping(ctx context.Context) error
	SaveAdjourned(ctx context.Context, a adjourned) error
	Adjourned(ctx context.Context, playerID string) ([]adjourned, error)
	DropAdjourned(ctx context.Context, id string) error
	DueAdjourned(ctx context.Context, before time.Time) ([]adjourned, error)
}

type userError string

func (e userError) Error() string { return string(e) }

const (
	errTaken       = userError("Ese nombre ya existe en ese país.")
	errCredentials = userError("Usuario, país o contraseña incorrectos.")
	errSession     = userError("La sesión caducó: vuelve a entrar.")
	errBadName     = userError("El nombre debe tener entre 3 y 20 letras, números, «_», «.» o «-».")
	errBadCountry  = userError("Elige un país.")
	errBadPassword = userError("La contraseña debe tener al menos 6 caracteres.")
	errBadEmail    = userError("Ese correo no es válido.")
	errEmailTaken  = userError("Ese correo ya está en uso.")
)

type account struct {
	Name     string `json:"name"`
	Country  string `json:"country"`
	Password string `json:"password"`
	Email    string `json:"email"`
}

type player struct {
	ID      string
	Name    string
	Country string
}

type session struct {
	AccessToken  string `json:"accessToken"`
	RefreshToken string `json:"refreshToken"`
	ExpiresIn    int    `json:"expiresIn"`
	Name         string `json:"name"`
	Country      string `json:"country"`
}

type result struct {
	Mode      string
	Settings  map[string]any
	StartedAt time.Time
	Players   []placing
}

type placing struct {
	UserID     string   `json:"user_id,omitempty"`
	Name       string   `json:"name"`
	Score      int      `json:"score"`
	Result     string   `json:"result"`
	Place      int      `json:"place"`
	BestBingo  int      `json:"best_bingo"`
	BestPlay   int      `json:"best_play"`
	Efficiency *float64 `json:"efficiency,omitempty"`
}

type statsRow struct {
	Mode          string   `json:"mode"`
	Games         int      `json:"games"`
	Wins          int      `json:"wins"`
	Losses        int      `json:"losses"`
	Draws         int      `json:"draws"`
	BestBingo     int      `json:"best_bingo"`
	BestPlay      int      `json:"best_play"`
	AvgScore      float64  `json:"avg_score"`
	AvgEfficiency *float64 `json:"avg_efficiency"`
}

var (
	namePattern    = regexp.MustCompile(`^[\p{L}\p{N}_.\-]{3,20}$`)
	countryPattern = regexp.MustCompile(`^[A-Z]{2}$`)
	emailPattern   = regexp.MustCompile(`^[^@\s]+@[^@\s]+\.[^@\s]+$`)
)

func (a account) normalized() account {
	a.Name = strings.TrimSpace(a.Name)
	a.Country = strings.ToUpper(strings.TrimSpace(a.Country))
	a.Email = strings.TrimSpace(a.Email)
	return a
}

func (a account) check(signingUp bool) error {
	switch {
	case !namePattern.MatchString(a.Name):
		return errBadName
	case !countryPattern.MatchString(a.Country):
		return errBadCountry
	case signingUp && len(a.Password) < 6:
		return errBadPassword
	case a.Email != "" && !emailPattern.MatchString(a.Email):
		return errBadEmail
	}
	return nil
}

func nameKey(name string) string {
	return strings.ToLower(name)
}

func asUserError(err error, fallback userError) error {
	var u userError
	if errors.As(err, &u) {
		return u
	}
	return fallback
}
