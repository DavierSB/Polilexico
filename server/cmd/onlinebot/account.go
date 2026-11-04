package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
)

type credentials struct {
	Name     string `json:"name"`
	Country  string `json:"country"`
	Password string `json:"password"`
}

func token(o options) (string, error) {
	c := credentials{o.name, o.country, o.password}
	if t, err := post(o.server+"/login", c); err == nil {
		return t, nil
	}
	return post(o.server+"/signup", c)
}

func post(url string, c credentials) (string, error) {
	body, _ := json.Marshal(c)
	res, err := http.Post(url, "application/json", bytes.NewReader(body))
	if err != nil {
		return "", err
	}
	defer res.Body.Close()
	var out struct {
		AccessToken string `json:"accessToken"`
		Error       string `json:"error"`
	}
	json.NewDecoder(res.Body).Decode(&out)
	if out.AccessToken == "" {
		return "", fmt.Errorf("%s: %d %s", url, res.StatusCode, out.Error)
	}
	return out.AccessToken, nil
}
