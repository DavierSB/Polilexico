package main

import (
	"os"
	"path/filepath"

	"lexico/engine"
)

func startEngine(cfg config) error {
	saves, err := os.MkdirTemp("", "lexico-saves-")
	if err != nil {
		return err
	}
	data := cfg.dataDir
	if data == "" {
		if data, err = linkRepoData(cfg.thirdParty); err != nil {
			return err
		}
	}
	return engine.Init(data, saves)
}

func linkRepoData(thirdParty string) (string, error) {
	root, err := filepath.Abs(thirdParty)
	if err != nil {
		return "", err
	}
	data, err := os.MkdirTemp("", "lexico-data-")
	if err != nil {
		return "", err
	}
	links := map[string]string{
		"lexica/gaddag":       filepath.Join(root, "woogles-lexica"),
		"letterdistributions": filepath.Join(root, "macondo", "data", "letterdistributions"),
		"strategy":            filepath.Join(root, "macondo", "data", "strategy"),
	}
	for name, target := range links {
		if err := link(filepath.Join(data, name), target); err != nil {
			return "", err
		}
	}
	return data, nil
}

func link(path, target string) error {
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return err
	}
	return os.Symlink(target, path)
}
