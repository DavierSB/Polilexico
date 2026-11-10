package main

import "net/http"

func withCORS(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("Origin") != "" {
			allowAnyOrigin(w.Header())
		}
		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next.ServeHTTP(w, r)
	})
}

func allowAnyOrigin(h http.Header) {
	h.Set("Access-Control-Allow-Origin", "*")
	h.Set("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS")
	h.Set("Access-Control-Allow-Headers", "Authorization, Content-Type, If-None-Match")
	h.Set("Access-Control-Expose-Headers", "ETag")
	h.Set("Access-Control-Max-Age", "86400")
}
