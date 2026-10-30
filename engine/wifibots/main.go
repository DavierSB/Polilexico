package main

import (
	"flag"
	"strings"
	"sync"
	"time"
)

func main() {
	role := flag.String("role", "dupguest", "dupguest | duphost | classicguest | classichost")
	addr := flag.String("addr", "127.0.0.1:47475", "dirección")
	names := flag.String("names", "Beto,Carla", "nombres de los bots")
	styles := flag.String("styles", "best,invalid", "estilo de cada bot")
	readyMs := flag.String("ready", "2000,9000", "espera antes de 'listo' (ms)")
	drop := flag.Int("drop", 0, "ronda tras la que el último bot se desconecta y vuelve")
	turn := flag.Int("turn", 40000, "ms por ronda (duphost)")
	rounds := flag.Int("rounds", 3, "rondas (duphost)")
	live := flag.Bool("live", true, "puntuaciones en vivo (duphost)")
	flag.Parse()
	initEngine()
	ns, ss, rs := strings.Split(*names, ","), strings.Split(*styles, ","), strings.Split(*readyMs, ",")
	switch *role {
	case "dupguest":
		var wg sync.WaitGroup
		for i, n := range ns {
			wg.Add(1)
			d := *drop
			if i != len(ns)-1 {
				d = 0
			}
			go func(n, s string, r time.Duration, d int) {
				defer wg.Done()
				runDupGuest(*addr, n, s, r, d)
			}(n, at(ss, i), ms(at(rs, i)), d)
			time.Sleep(500 * time.Millisecond)
		}
		wg.Wait()
	case "duphost":
		runDupHost(*addr, ns, ss, rs, *turn, *rounds, *live)
	case "classicguest":
		runClassicGuest(*addr, ns[0])
	case "classichost":
		runClassicHost(*addr, ns[0], *turn)
	}
}

func at(s []string, i int) string {
	if i < len(s) {
		return s[i]
	}
	return s[len(s)-1]
}

func ms(s string) time.Duration {
	var n int
	for _, c := range s {
		n = n*10 + int(c-'0')
	}
	return time.Duration(n) * time.Millisecond
}
