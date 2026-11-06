package main

import (
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
)

func main() {
	cfg := loadConfig()
	if err := startEngine(cfg); err != nil {
		log.Fatal(err)
	}
	store := openStore(cfg)
	h := newHub(store)
	go h.sweepForever()
	go adjournOnShutdown(h)
	log.Printf("escuchando en :%s (%s)", cfg.port, store.Name())
	log.Fatal(http.ListenAndServe(":"+cfg.port, routes(h, store)))
}

func adjournOnShutdown(h *hub) {
	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGTERM, syscall.SIGINT)
	<-stop
	log.Printf("apagando: aplazadas %d partidas en curso", h.adjournAll())
	os.Exit(0)
}
