package app.lexico.ui.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoftKeysTest {
  @Test fun lowercaseLettersAreSingleTiles() {
    assertEquals(listOf("C", "L", "R", "Ñ"), "clrñ".map(::tileOf))
  }

  @Test fun shiftedCLRAreDigraphs() {
    assertEquals(listOf("CH", "LL", "RR", "A"), "CLRA".map(::tileOf))
  }

  @Test fun questionMarkOrSpaceIsABlankAndOthersAreIgnored() {
    assertEquals("?", tileOf('?'))
    assertEquals("?", tileOf(' '))
    assertNull(tileOf('k'))
    assertNull(tileOf('1'))
  }
}
