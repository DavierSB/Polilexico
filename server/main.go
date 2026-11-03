package main

import (
	"log"
	"net/http"
)

func main() {
	cfg := loadConfig()
	if err := startEngine(cfg); err != nil {
		log.Fatal(err)
	}
	store := openStore(cfg)
	log.Printf("escuchando en :%s (%s)", cfg.port, store.Name())
	log.Fatal(http.ListenAndServe(":"+cfg.port, routes(newHub(store), store)))
}
