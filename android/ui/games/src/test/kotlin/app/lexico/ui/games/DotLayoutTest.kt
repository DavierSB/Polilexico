package app.lexico.ui.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class DotLayoutTest {
  private val frame = StripFrame(left = 0f, right = 400f, bottom = 88f, step = 16f, labelHeight = 16f)

  @Test
  fun loneDotsKeepTheirXAndHaveNoLabel() {
    val layout = layoutDots(listOf(50f, 200f), listOf(10, 40), frame)
    assertEquals(listOf(50f, 200f), layout.spots.map { it.x })
    assertTrue(layout.labels.isEmpty())
  }

  @Test
  fun equalValuesSpreadInARowCentredOnTheValue() {
    val layout = layoutDots(List(3) { 200f }, List(3) { 90 }, frame)
    assertEquals(listOf(184f, 200f, 216f), layout.spots.map { it.x })
    assertEquals(1, layout.spots.map { it.y }.distinct().size)
    assertEquals(listOf(GroupLabel(90, 90, 200f, 72f)), layout.labels)
  }

  @Test
  fun nearValuesJoinOneGroupWithARangeLabel() {
    val layout = layoutDots(listOf(100f, 108f), listOf(87, 88), frame)
    assertEquals(GroupLabel(87, 88, 104f, 72f), layout.labels.single())
    assertTrue(layout.spots[1].x - layout.spots[0].x >= frame.step)
  }

  @Test
  fun crowdedGroupStacksRows() {
    val layout = layoutDots(List(7) { 200f }, List(7) { 100 }, frame)
    assertEquals(listOf(80f, 64f), layout.spots.map { it.y }.distinct())
    assertEquals(56f, layout.labels.single().bottom)
  }

  @Test
  fun manyDotsStayInsideTheFrameWithoutOverlapping() {
    val xs = List(40) { 400f } + List(5) { 395f }
    val layout = layoutDots(xs, xs.map { it.toInt() }, frame)
    assertTrue(layout.spots.all { it.x in 0f..400f && it.y - frame.step / 2 >= frame.labelHeight })
    assertTrue(layout.labels.all { it.bottom - frame.labelHeight >= 0f })
    assertTrue(minDistance(layout.spots) >= frame.step - 0.01f)
  }

  @Test
  fun groupsNeverTouchEachOther() {
    val xs = listOf(100f, 100f, 100f, 100f, 140f, 140f, 140f)
    val layout = layoutDots(xs, xs.map { it.toInt() }, frame)
    assertTrue(minDistance(layout.spots) >= frame.step - 0.01f)
  }

  private fun minDistance(spots: List<DotSpot>): Float =
    spots.indices.flatMap { i -> (i + 1 until spots.size).map { j -> hypot(spots[i].x - spots[j].x, spots[i].y - spots[j].y) } }
      .minOrNull() ?: Float.MAX_VALUE
}
