FROM golang:1.26.1-bookworm AS build
ARG MACONDO_COMMIT=c701af1e37bb6f96d8dda1e5703ac2b2e6d6fca7
WORKDIR /src
RUN git clone --filter=blob:none https://github.com/domino14/macondo.git third_party/macondo \
 && git -C third_party/macondo checkout "$MACONDO_COMMIT"
COPY third_party/woogles-lexica/ third_party/woogles-lexica/
RUN third_party/woogles-lexica/download.sh
COPY engine/ engine/
COPY server/ server/
RUN cd server && CGO_ENABLED=0 go build -ldflags="-s -w" -o /out/server .
RUN mkdir -p /out/data/lexica/gaddag /out/data/letterdistributions /out/data/strategy/default \
 && cp third_party/woogles-lexica/FILE2017.kwg third_party/woogles-lexica/FILE2017.klv2 /out/data/lexica/gaddag/ \
 && cp third_party/macondo/data/letterdistributions/spanish /out/data/letterdistributions/ \
 && cp third_party/macondo/data/strategy/default/preendgame.json third_party/macondo/data/strategy/default/quackle_preendgame.json \
       third_party/macondo/data/strategy/default/winpct.csv /out/data/strategy/default/

FROM alpine:3.20
RUN apk add --no-cache ca-certificates && adduser -D polilexico
COPY --from=build /out /app
USER polilexico
ENV LEXICO_DATA=/app/data PORT=8080
EXPOSE 8080
CMD ["/app/server"]
