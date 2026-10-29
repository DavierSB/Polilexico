package app.lexico.model.dictionary

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryTest {
  private val dictionary = Dictionary(directory())

  @Test
  fun directWordWithoutLineUsesItsOwnSenses() {
    val meaning = dictionary.meanings("gata").single()
    assertEquals("gata", meaning.lemma)
    assertEquals("", meaning.relation)
    assertEquals("Hembra del gato.", meaning.senses.single().text)
  }

  @Test
  fun inflectionNamesItsLemmaWithAccents() {
    val meanings = dictionary.meanings("cancion")
    assertEquals("canción", meanings.single().lemma)
    assertEquals(listOf("Composición en verso."), meanings.single().senses.map { it.text })
  }

  @Test
  fun relationReadsThroughTheMiddleWord() {
    val meaning = dictionary.meanings("gatas").single()
    assertEquals("Plural de gata", meaning.relation)
  }

  @Test
  fun verbFormsShowVerbSensesFirst() {
    val meaning = dictionary.meanings("Cacen").single()
    assertEquals("Conjugación (subjuntivo) de chazar", meaning.relation)
    assertEquals(Category.VERB, meaning.senses.first().category)
  }

  @Test
  fun demonymComesFirst() {
    val meanings = dictionary.meanings("quio")
    assertEquals("Natural de Quío", meanings.first().senses.single().text)
    assertEquals(Source.DEMONYMS, meanings.first().senses.single().source)
  }

  @Test
  fun nearlyEqualSensesShowOnce() {
    assertEquals(listOf("Unidad monetaria de Fiyi, dividida en cien centavos."), dictionary.meanings("dolar").flatMap { it.senses }.map { it.text })
  }

  @Test
  fun summaryDropsContextAndTail() {
    assertEquals("Forma de aceptar cualquier canto", Summary.line("En el juego del truco, forma de aceptar cualquier canto. Se usa poco."))
    assertEquals("Planta herbácea de ciclo anual", Summary.line("Planta herbácea de ciclo anual, de origen desconocido pero hoy cosmopolita, de flores rojas."))
    assertEquals("Hembra del gato", Summary.line("Hembra del gato."))
  }

  @Test
  fun unknownWordHasNoMeanings() {
    assertEquals(emptyList<Meaning>(), dictionary.meanings("xyz"))
  }

  private fun directory(): File {
    val dir = Files.createTempDirectory("dictionary").toFile()
    File(dir, Dictionary.RELATIONS).writeText("directa\t\t\nflexion\tplural\tnominal\nflexion\tconjugación (subjuntivo)\tverbo\n")
    File(dir, Dictionary.WORDS).writeText("Cacen\t3zar,2\ncancion\t0,0\ngatas\t1,1\n")
    File(dir, Dictionary.DEMONYMS).writeText("quio\tNatural de Quío\n")
    File(dir, Dictionary.SENSES).writeText(
      "cancion\tcanción\tws\tnoun\tComposición en verso.\n" +
        "chazar\t\tws\tnoun\tUn chazar.\t\twv\tverb\tChazar la pelota.\n" +
        "dolar\tdólar\tws\tnoun\tUnidad monetaria de Fiyi, dividida en cien centavos.\t\tws\tnoun\tUnidad monetaria de Belice, dividida en cien centavos.\n" +
        "gata\t\tws\tnoun\tHembra del gato.\n",
    )
    return dir
  }
}
