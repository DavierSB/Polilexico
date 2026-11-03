package main

import (
	"os"
	"strings"
)

type config struct {
	port        string
	dataDir     string
	thirdParty  string
	supabaseURL string
	secretKey   string
}

func loadConfig() config {
	return config{
		port:        env("PORT", "8080"),
		dataDir:     os.Getenv("LEXICO_DATA"),
		thirdParty:  env("LEXICO_THIRD_PARTY", "../third_party"),
		supabaseURL: strings.TrimRight(os.Getenv("SUPABASE_URL"), "/"),
		secretKey:   os.Getenv("SUPABASE_SECRET_KEY"),
	}
}

func env(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func openStore(cfg config) store {
	if cfg.supabaseURL == "" || cfg.secretKey == "" {
		return newMemoryStore()
	}
	return newSupabaseStore(cfg.supabaseURL, cfg.secretKey)
}
