package app.lexico.games.export

import org.junit.Assert.assertEquals
import org.junit.Test

class GcgNamesTest {
  @Test fun botsTakeTheirPolilexicoNames() {
    val gcg = "#player1 hastybot HastyBot\n#player2 tu Tú\n>hastybot: AAEEÑOS 8H AÑO +20 20\n>tu: ABC 9H AB +5 5\n"
    assertEquals(
      "#player1 gitana Gitana\n#player2 tu Tú\n>gitana: AAEEÑOS 8H AÑO +20 20\n>tu: ABC 9H AB +5 5\n",
      withBotAliases(gcg),
    )
  }

  @Test fun aliasNicknamesHaveNoSpacesOrAccents() {
    assertEquals("#player2 mariam María M.\n>mariam: A", withBotAliases("#player2 steebot STEEBot\n>steebot: A"))
  }
}
