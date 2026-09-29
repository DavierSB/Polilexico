# woogles-lexica

El diccionario con el que juega el motor:

- `*.kwg`: la lista de palabras como grafo (GADDAG), para validar y generar jugadas.
- `*.klv2`: el valor de cada "leave" (lo que queda en el atril), para la equity del bot.

No se versionan: `sources.txt` guarda la URL y el SHA-256 de cada uno, y `download.sh` los baja
si faltan. `android/engine-bridge/build_engine.sh` lo llama antes de compilar.

Para cambiar de versión: actualizar URL y SHA-256 en `sources.txt`.
