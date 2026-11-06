package main

import (
	"flag"
	"log"
	"os"
	"time"
)

type options struct {
	server     string
	name       string
	country    string
	password   string
	thirdParty string
	delay      time.Duration
	delayMax   time.Duration
	dropEvery  int
	dropFor    time.Duration
	create     string
	accept     bool
	players    int
	rounds     int
	turn       time.Duration
	clock      time.Duration
	games      int
	style      string
	drop       int
	chatEvery  time.Duration
	challenge  bool
	from       string
	ignore     string
	adjourn    bool
	proposeAt  int
	again      time.Duration
}

func main() {
	o := parseOptions()
	log.SetPrefix(o.name + " ")
	initEngine(o.thirdParty)
	log.Printf("contra %s", o.server)
	state := &progress{}
	for !state.done(o) {
		if err := session(o, state); err != nil {
			log.Printf("desconectado: %v", err)
		}
		time.Sleep(state.pause())
	}
	log.Printf("terminó sus %d partidas", o.games)
	os.Exit(0)
}

func parseOptions() options {
	var o options
	flag.StringVar(&o.server, "server", "https://polilexico1.onrender.com", "servidor")
	flag.StringVar(&o.name, "name", "Robot", "usuario del bot")
	flag.StringVar(&o.country, "country", "CU", "país del bot")
	flag.StringVar(&o.password, "password", "robot-polilexico", "contraseña del bot")
	flag.StringVar(&o.thirdParty, "third-party", "../third_party", "carpeta third_party del repo")
	flag.DurationVar(&o.delay, "delay", 3*time.Second, "espera antes de cada jugada")
	flag.DurationVar(&o.delayMax, "delay-max", 0, "si es mayor que -delay, la espera es al azar entre ambas")
	flag.IntVar(&o.dropEvery, "drop-every", 0, "se desconecta tras cada N jugadas suyas")
	flag.DurationVar(&o.dropFor, "drop-for", 3*time.Second, "cuánto tarda en volver tras desconectarse a propósito")
	flag.StringVar(&o.create, "create", "", "publica búsquedas: classic, duplicate o classic,duplicate")
	flag.BoolVar(&o.accept, "accept", true, "acepta retos y búsquedas ajenas")
	flag.IntVar(&o.players, "players", 2, "duplicada: jugadores para empezar")
	flag.IntVar(&o.rounds, "rounds", 0, "duplicada: rondas (0 = hasta el final)")
	flag.DurationVar(&o.turn, "turn", 30*time.Second, "duplicada: tiempo por ronda")
	flag.DurationVar(&o.clock, "clock", 10*time.Minute, "clásica: reloj de cada jugador")
	flag.IntVar(&o.games, "games", 0, "partidas antes de salir (0 = sin fin)")
	flag.StringVar(&o.style, "style", "best", "best | mixed (a veces inválida o pase) | random (al azar entre las 20 mejores)")
	flag.IntVar(&o.drop, "drop", 0, "se desconecta una vez tras esa jugada suya")
	flag.DurationVar(&o.chatEvery, "chat", 0, "manda un mensaje de chat cada tanto en partida (0 = nunca)")
	flag.BoolVar(&o.challenge, "challenge", false, "reta una vez a cada conectado libre")
	flag.StringVar(&o.from, "from", "", "solo acepta búsquedas de este anfitrión")
	flag.StringVar(&o.ignore, "ignore", "", "no acepta búsquedas de anfitriones que empiecen así")
	flag.BoolVar(&o.adjourn, "adjourn", true, "acepta las propuestas de aplazar")
	flag.IntVar(&o.proposeAt, "propose-at", 0, "propone aplazar al empezar esa ronda o vuelta (0 = nunca)")
	flag.DurationVar(&o.again, "propose-again", 15*time.Second, "vuelve a proponer aplazar tras este tiempo")
	flag.Parse()
	return o
}

type progress struct {
	finished int
	dropped  bool
	said     int
	mine     int
	away     time.Duration
	proposed bool
}

func (p *progress) pause() time.Duration {
	if p.away > 0 {
		d := p.away
		p.away = 0
		return d
	}
	return 3 * time.Second
}

func (p *progress) done(o options) bool {
	return o.games > 0 && p.finished >= o.games
}
