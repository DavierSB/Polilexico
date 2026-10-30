package main

import (
	"os"
	"path/filepath"

	"lexico/engine"
)

const thirdParty = "/home/davier/Private/Lexico/third_party"

func initEngine() {
	root, _ := os.MkdirTemp("", "wifibots-")
	data := filepath.Join(root, "data")
	links := map[string]string{
		"lexica/gaddag":       filepath.Join(thirdParty, "woogles-lexica"),
		"letterdistributions": filepath.Join(thirdParty, "macondo", "data", "letterdistributions"),
		"strategy":            filepath.Join(thirdParty, "macondo", "data", "strategy"),
	}
	for name, target := range links {
		p := filepath.Join(data, name)
		os.MkdirAll(filepath.Dir(p), 0o755)
		os.Symlink(target, p)
	}
	if err := engine.Init(data, filepath.Join(root, "saves")); err != nil {
		panic(err)
	}
}
