package endgame

var pauseHook func()

func SetPause(f func()) {
	pauseHook = f
}

func pause() {
	if pauseHook != nil {
		pauseHook()
	}
}
