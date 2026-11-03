package main

import (
	"encoding/json"
	"errors"
	"log"
	"net/http"
	"strings"
)

const maxBackupBytes = 16 << 20

func routes(h *hub, st store) http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", health(st))
	mux.HandleFunc("GET /ws", h.serve)
	mux.HandleFunc("POST /signup", signUp(st))
	mux.HandleFunc("POST /login", logIn(st))
	mux.HandleFunc("POST /refresh", refresh(st))
	mux.HandleFunc("GET /stats", authed(st, stats(st)))
	mux.HandleFunc("GET /backup", authed(st, backup(st)))
	mux.HandleFunc("PUT /backup", authed(st, saveBackup(st)))
	return mux
}

type playerHandler func(w http.ResponseWriter, r *http.Request, p player)

func health(st store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Query().Has("db") {
			if err := st.Ping(r.Context()); err != nil {
				http.Error(w, err.Error(), http.StatusServiceUnavailable)
				return
			}
		}
		w.Write([]byte("ok"))
	}
}

func signUp(st store) http.HandlerFunc {
	return withAccount(true, func(w http.ResponseWriter, r *http.Request, a account) {
		reply(w)(st.SignUp(r.Context(), a))
	})
}

func logIn(st store) http.HandlerFunc {
	return withAccount(false, func(w http.ResponseWriter, r *http.Request, a account) {
		reply(w)(st.LogIn(r.Context(), a))
	})
}

func refresh(st store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var body struct{ RefreshToken string }
		if decode(w, r, &body) {
			reply(w)(st.Refresh(r.Context(), body.RefreshToken))
		}
	}
}

func stats(st store) playerHandler {
	return func(w http.ResponseWriter, r *http.Request, p player) {
		rows, err := st.Stats(r.Context(), p)
		reply(w)(map[string]any{"name": p.Name, "country": p.Country, "rows": rows}, err)
	}
}

func backup(st store) playerHandler {
	return func(w http.ResponseWriter, r *http.Request, p player) {
		data, err := st.Backup(r.Context(), p)
		reply(w)(map[string]string{"data": data}, err)
	}
}

func saveBackup(st store) playerHandler {
	return func(w http.ResponseWriter, r *http.Request, p player) {
		r.Body = http.MaxBytesReader(w, r.Body, maxBackupBytes)
		var body struct{ Data string }
		if decode(w, r, &body) {
			reply(w)(map[string]bool{"ok": true}, st.SaveBackup(r.Context(), p, body.Data))
		}
	}
}

func withAccount(signingUp bool, next func(http.ResponseWriter, *http.Request, account)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var a account
		if !decode(w, r, &a) {
			return
		}
		a = a.normalized()
		if err := a.check(signingUp); err != nil {
			reply(w)(nil, err)
			return
		}
		next(w, r, a)
	}
}

func authed(st store, next playerHandler) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		p, err := st.Identify(r.Context(), strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer "))
		if err != nil {
			writeJSON(w, http.StatusUnauthorized, map[string]string{"error": errSession.Error()})
			return
		}
		next(w, r, p)
	}
}

func decode(w http.ResponseWriter, r *http.Request, v any) bool {
	if err := json.NewDecoder(r.Body).Decode(v); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "Petición inválida."})
		return false
	}
	return true
}

func reply(w http.ResponseWriter) func(any, error) {
	return func(v any, err error) {
		var u userError
		switch {
		case errors.As(err, &u):
			writeJSON(w, http.StatusBadRequest, map[string]string{"error": u.Error()})
		case err != nil:
			log.Printf("error: %v", err)
			writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "No se pudo completar. Inténtalo de nuevo."})
		default:
			writeJSON(w, http.StatusOK, v)
		}
	}
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(v)
}
