package app.lexico.soundboard

import android.content.Context

data class Variant(val file: String, val pitch: Float = 1f, val maxMs: Long? = null, val times: Int = 1) {
  val label: String get() = listOfNotNull(file, pitchLabel(), maxMs?.let { "primeros $it ms" }, "dos veces".takeIf { times == 2 })
    .joinToString(" · ")

  private fun pitchLabel(): String? = if (pitch < 1f) "más grave (${(pitch * 100).toInt()} %)" else null
}

val CHOSEN: Map<Slot, Variant> = mapOf(
  Slot.WARNING to Variant("material_state_change_confirm_down"),
  Slot.LAST_SECONDS to Variant("material_alert_simple"),
  Slot.TIME_UP to Variant("material_alert_high_intensity", pitch = 0.8f),
  Slot.CORRECT to Variant("material_state_change_confirm_up"),
  Slot.MISS to Variant("material_state_change_confirm_up", pitch = 0.8f),
  Slot.WRONG to Variant("material_alert_error_01"),
  Slot.OPPONENT to Variant("material_ui_tap_variant_01"),
  Slot.CELEBRATION to Variant("material_hero_decorative_celebration_01"),
  Slot.GAME_OVER to Variant("material_hero_simple_celebration_01"),
  Slot.CLICK to Variant("material_ui_tap_variant_01"),
)

val PENDING: Map<Slot, List<Variant>> = emptyMap()

fun rawSound(ctx: Context, file: String): Int? =
  ctx.resources.getIdentifier(file, "raw", ctx.packageName).takeIf { it != 0 }
