package main

import (
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestPreflightAllowsBrowsers(t *testing.T) {
	r := httptest.NewRequest(http.MethodOptions, "/login", nil)
	r.Header.Set("Origin", "https://polilexico.com")
	w := httptest.NewRecorder()
	withCORS(http.NotFoundHandler()).ServeHTTP(w, r)
	if w.Code != http.StatusNoContent || w.Header().Get("Access-Control-Allow-Origin") != "*" {
		t.Fatalf("preflight: %d %q", w.Code, w.Header().Get("Access-Control-Allow-Origin"))
	}
}

func TestPlainRequestsKeepWorking(t *testing.T) {
	r := httptest.NewRequest(http.MethodGet, "/health", nil)
	r.Header.Set("Origin", "https://polilexico.com")
	w := httptest.NewRecorder()
	withCORS(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) { w.Write([]byte("ok")) })).ServeHTTP(w, r)
	if w.Body.String() != "ok" || w.Header().Get("Access-Control-Allow-Origin") != "*" {
		t.Fatalf("get: %q %q", w.Body.String(), w.Header().Get("Access-Control-Allow-Origin"))
	}
}
