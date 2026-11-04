# server

El servidor de Polilexico en línea: arbitra las partidas entre jugadores por internet. Está en Go
y usa el mismo [`engine`](../engine/README.md) que la app, así que las reglas son idénticas.

- **Clásica:** la partida es un `classic.Match` del motor. Cada jugador recibe solo lo que puede
  ver: su atril, el tablero y las fichas por salir, nunca el atril del rival.
- **Duplicada:** la mesa es la misma `duplicate.Table` de la duplicada por wifi.
- **Búsquedas y retos:** se publica una búsqueda que ven todos los conectados, se acepta la de
  otro o se reta directamente a un jugador conectado.
- **Cuentas:** usuario + país + contraseña, correo opcional. Con Supabase (Auth y Postgres);
  sin Supabase, las cuentas viven en memoria.

## Probar en el PC

```bash
source ../env.sh
cd server
go run .
```

Escucha en el puerto 8080 con cuentas en memoria, que se pierden al cerrarlo.

```bash
go test -race ./...
```

## Desplegar

1. **Supabase:** pegar [`db/schema.sql`](db/schema.sql) en el *SQL Editor* y ejecutarlo (en un proyecto
   ya creado, solo lo nuevo; por ejemplo la tabla `photos` de las fotos de perfil). En
   *Authentication → Sign In / Providers → Email*, desactivar *Confirm email*.
2. **Render:** *New → Blueprint* con este repositorio; [`render.yaml`](../render.yaml) crea el
   servicio gratis en Oregon. Pide dos variables:
   - `SUPABASE_URL`: la *Project URL* de Supabase.
   - `SUPABASE_SECRET_KEY`: la clave `sb_secret_...` de Supabase. Solo va en Render, nunca en el
     repositorio ni en la app.
3. **GitHub:** en *Settings → Secrets and variables → Actions → Variables*, crear
   `POLILEXICO_SERVER` con la dirección de Render (por ejemplo `https://polilexico1.onrender.com`).
   El flujo [`keepalive.yml`](../.github/workflows/keepalive.yml) la visita cada 10 minutos para
   que no se duerman ni Render ni Supabase.

La dirección por defecto de la app está en `DEFAULT_SERVER`
([`OnlineStore.kt`](../android/app/src/main/kotlin/app/lexico/online/OnlineStore.kt)) y se puede
cambiar desde la pantalla de cuenta (*Servidor*).

## Protocolo

HTTP para las cuentas (`/signup`, `/login`, `/refresh`), las estadísticas (`/stats`) y la copia de
las estadísticas offline (`/backup`); `/health` para Render y el cron. Las partidas van por un
WebSocket en `/ws` con mensajes JSON de una línea (`hello`, `create`, `challenge`, `join`, `move`…),
descritos en [`protocol.go`](protocol.go).
