package app.lexico.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationsTest {
  @Test fun format() {
    assertEquals("20:00", Durations.format(20 * 60_000L))
    assertEquals("0:07", Durations.format(6_100))
    assertEquals("0:00", Durations.format(-5_000))
  }

  @Test fun parse() {
    assertEquals(20 * 60_000L, Durations.parse("20"))
    assertEquals(200_000L, Durations.parse("3:20"))
    assertEquals(45_000L, Durations.parse(" 0:45 "))
    assertNull(Durations.parse("3:75"))
    assertNull(Durations.parse("tres"))
    assertNull(Durations.parse("1:2:3"))
  }
}
