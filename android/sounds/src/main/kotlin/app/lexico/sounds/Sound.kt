package app.lexico.sounds

enum class Sound(val res: Int, val title: String, val use: String, val pitch: Float = 1f) {
  WARNING(R.raw.material_state_change_confirm_down, "Aviso de tiempo", "Quedan 30 s en duplicada y Sprint; queda 1 minuto en la clásica."),
  LAST_SECONDS(R.raw.material_alert_simple, "Últimos segundos", "Quedan 5 s en duplicada y Sprint."),
  TIME_UP(R.raw.material_alert_high_intensity, "Tiempo agotado", "En la clásica, empieza el descuento o se pierde por tiempo; en Sprint, se acaba la serie.", 0.8f),
  CORRECT(R.raw.material_state_change_confirm_up, "Acierto", "Mano resuelta en Sprint, palabra recordada; en duplicada, ronda en la que se acertó la jugada del máster."),
  MISS(R.raw.material_state_change_confirm_up, "Sin acierto", "Fin de una ronda de duplicada sin acertar la jugada del máster.", 0.8f),
  WRONG(R.raw.material_alert_error_01, "Error", "Palabras no válidas o rendirse en Sprint, palabra mal recordada."),
  OPPONENT(R.raw.material_ui_tap_variant_01, "Jugada del rival", "El bot terminó su jugada en la clásica."),
  CELEBRATION(R.raw.material_hero_decorative_celebration_01, "Celebración", "Con el confeti de ¡Ganaste!, y en cada récord nuevo."),
  GAME_OVER(R.raw.material_hero_simple_celebration_01, "Fin de partida", "Fin de la duplicada sin récord."),
  CLICK(R.raw.material_ui_tap_variant_01, "Clic", "Botones que cambian de pantalla y los del menú lateral."),
}
