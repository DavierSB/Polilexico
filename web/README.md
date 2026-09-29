# web

La versión web de Poliléxico. Todavía no está empezada.

Usará el mismo motor ([`engine/`](../engine/README.md)) por uno de estos dos caminos, a decidir:

- **Servidor en Go** que usa `engine/` como biblioteca y le habla al navegador por HTTP o
  WebSocket. El navegador solo dibuja.
- **`engine/` compilado a WebAssembly** y ejecutado en el navegador, sin servidor. Pesa más (el
  motor más los ~9 MB del diccionario), pero funciona sin conexión.

Como en Android, las reglas del juego no se reescriben aquí: la web solo llama al motor, dibuja
y guarda.
