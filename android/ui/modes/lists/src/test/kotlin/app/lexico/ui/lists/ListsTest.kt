package app.lexico.ui.lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ListsTest {
  private val lexicon = Lexicon.parse(
    listOf("v\t900\t999\trecazar:id\tcazar recazar", "n\t500\t500\tm, mp\thoz hoces", "n\t0\t-1\tm\tcheque"),
    { listOf("v\t999\t0\tcaza", "n\t750\t1\thoz", "n\t500\t1\thoces", "v\t250\t0\tcazar", "n\t-1\t2\tCeque") },
    { listOf("1casar 5cozar", "", "") },
  )

  @Test
  fun parsesDigraphsAsOneTile() {
    assertEquals("Ceque", WordKeys.parse("cheque"))
    assertEquals("ch", WordKeys.parse("c h").let(WordKeys::show))
    assertEquals(listOf("CH", "E"), WordKeys.tiles("Ce"))
    assertEquals("Ce", WordKeys.of(listOf("CH", "E")))
  }

  @Test
  fun simpleLettersExcludeDigraphsAndRareLetters() {
    val simple = Filter(simpleOnly = true)
    assertTrue(simple.matches("cazar".replace('z', 's')))
    assertFalse(simple.matches("cazar"))
    assertFalse(simple.matches("Ceque"))
  }

  @Test
  fun familyPassesWhenOneWordMatches() {
    assertEquals(listOf("v:cazar"), lexicon.filter(FilterSpec(maxLength = 4, kind = Kind.VERBAL)).map { it.key })
    assertEquals(emptyList<String>(), lexicon.filter(FilterSpec(minLength = 5, kind = Kind.VERBAL)).map { it.key })
  }

  @Test
  fun placedWordReachesItsFamilies() {
    assertEquals(setOf("w:caza", "v:cazar"), lexicon.unitsOf("caza").map { it.key }.toSet())
    assertEquals("caza", lexicon.find("w:caza")?.head)
  }

  @Test
  fun specSurvivesEncoding() {
    val spec = FilterSpec(3, 7, "aerch", "c..", "..s", Kind.VERBAL, Order.LENGTH, 2, null, simpleOnly = true, words = true)
    assertEquals(spec, FilterSpec.decode(spec.encode()))
    assertNull(FilterSpec.decode("roto"))
  }

  @Test
  fun listCountsOnlyItsMembers() {
    val list = WordList("x", "Mi lista", 1L, FilterSpec(), listOf("v:cazar", "n:hoz")).learn(listOf("v:cazar", "w:caza"))
    assertEquals(50, list.percent)
    assertEquals(listOf("n:hoz"), list.pending)
    assertEquals(list, WordList.decode("x", list.encode()))
  }

  @Test
  fun inProgressNeedsALearnedWordAndRecentUseWhenBarelyStarted() {
    val members = (1..40).map { "n:w$it" }
    val day = 24 * 60 * 60 * 1000L
    val fresh = WordList("x", "Mi lista", 1L, FilterSpec(), members, studied = 5)
    assertFalse(fresh.inProgress(0L))
    val barely = fresh.learn(listOf("n:w1")).copy(used = 10 * day)
    assertTrue(barely.inProgress(12 * day))
    assertFalse(barely.inProgress(14 * day))
    assertTrue(barely.learn(listOf("n:w2")).inProgress(14 * day))
    assertEquals(barely, WordList.decode("x", barely.encode()))
  }

  @Test
  fun completingAListCountsEachTime() {
    val list = WordList("x", "Mi lista", 1L, FilterSpec(), listOf("v:cazar", "n:hoz")).learn(listOf("v:cazar"))
    assertTrue(list.inProgress(0L))
    val done = list.learn(listOf("n:hoz")).learn(listOf("n:hoz"))
    assertEquals(1, done.completions)
    assertEquals(2, done.restart().learn(listOf("v:cazar", "n:hoz")).completions)
    assertEquals(done, WordList.decode("x", done.encode()))
    assertEquals(1, CompletedStudies(listOf(done, list)).studies)
    assertEquals(2, CompletedStudies(listOf(done, list)).families)
  }

  @Test
  fun handsFavourImportantForms() {
    val forms = lexicon.forms(lexicon.find("v:cazar")!!)
    val picks = (1..2000).map { Hands.form(forms, Filter(), Random(it)) }.groupingBy { it }.eachCount()
    assertTrue(picks.getValue("caza") > picks.getValue("cazar"))
    assertEquals("cazar", Hands.form(forms, Filter(minLength = 5)))
  }

  @Test
  fun similarWordsCarryTheirRule() {
    assertEquals(listOf(Similar("casar", Likeness.SOUND), Similar("cozar", Likeness.VOWEL)), lexicon.similar(lexicon.find("v:cazar")!!))
    assertEquals(lexicon.similar(lexicon.find("v:cazar")!!), lexicon.similar(lexicon.find("w:caza")!!))
  }

  @Test
  fun verbTraitsBelongToEachVerb() {
    val cazar = lexicon.find("v:cazar")!!
    assertEquals(emptyList<VerbTrait>(), cazar.traits("cazar"))
    assertEquals(listOf(VerbTrait.INTRANSITIVE, VerbTrait.DEFECTIVE), cazar.traits("recazar"))
  }

  @Test
  fun defaultListsKeepTheirProgress() {
    val lines = listOf(
      "Verbos/Letra Z\tLetra Z, letras simples · 5 letras\t2|10|z|||VERBAL|POINTS||||f\tJefe de familia de 5 letras\tv:cazar",
      "NoVerbos\t\t2|10||||ALL|POINTS||||f\t\t",
    )
    val first = ListCatalog.parse(lines, emptyList())
    val list = (first.entries(listOf("Verbos", "Letra Z")).single() as CatalogEntry.Leaf).list
    val again = ListCatalog.parse(lines, listOf(list.learn(listOf("v:cazar"))))
    assertEquals(listOf("Verbos", "NoVerbos"), again.entries(emptyList()).map { (it as CatalogEntry.Sub).folder.name })
    assertEquals(100, again.find(list.id)!!.percent)
    assertTrue(list.isDefault)
  }

  @Test
  fun repeatedLetterReadsOnce() {
    assertEquals(listOf("Con la letra D duplicada"), FilterSpec(contains = "dd").summary())
  }

  @Test
  fun restOfLettersCanBeSimple() {
    val qAndSimple = Filter(contains = "q", simpleOnly = true)
    assertTrue(qAndSimple.matches("queso"))
    assertFalse(qAndSimple.matches("quemar"))
  }
}
