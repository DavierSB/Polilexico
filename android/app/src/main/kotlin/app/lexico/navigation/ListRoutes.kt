package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.lexico.game.Lexico
import app.lexico.menu.ThemeState
import app.lexico.modes.lists.WordLexicon
import app.lexico.modes.lists.engineHands
import app.lexico.modes.lists.listBook
import app.lexico.modes.lists.listCatalog
import app.lexico.ui.common.LocalWordLookup
import app.lexico.ui.lists.CreateListScreen
import app.lexico.ui.lists.Lexicon
import app.lexico.ui.lists.CatalogEntry
import app.lexico.ui.lists.CompletedScreen
import app.lexico.ui.lists.CompletedStudies
import app.lexico.ui.lists.Folder
import app.lexico.ui.lists.ListBook
import app.lexico.ui.lists.ListCatalog
import app.lexico.ui.lists.ListsPage
import app.lexico.ui.lists.ListScreen
import app.lexico.ui.lists.ListsScreen
import app.lexico.ui.lists.StudyScreen
import app.lexico.ui.lists.TestScreen
import app.lexico.ui.lists.TestSession
import app.lexico.ui.lists.WordList
import java.util.UUID

@Composable
internal fun NewListRoute(lexico: Lexico, nav: Navigator) {
  val book = remember { listBook(lexico.wordLists) }
  WithLexicon(lexico) { lexicon ->
    CreateListScreen(lexicon, nav::back) { name, spec, members ->
      book.save(WordList(UUID.randomUUID().toString(), name, System.currentTimeMillis(), spec, members))
      nav.replace(Screen.ListFolder(listOf(ListCatalog.MINE)))
    }
  }
}

@Composable
internal fun WordListsRoute(lexico: Lexico, nav: Navigator) {
  val page = remember {
    val book = listBook(lexico.wordLists)
    val mine = CatalogEntry.Sub(Folder.of(listOf(ListCatalog.MINE), book.all().filterNot { it.isDefault }))
    ListsPage("Listas", listCatalog(lexico, book).entries(emptyList()) + mine)
  }
  ListsPage(page, nav)
}

@Composable
internal fun ListFolderRoute(screen: Screen.ListFolder, lexico: Lexico, nav: Navigator) {
  val page = remember(screen) {
    val book = listBook(lexico.wordLists)
    if (screen.path == listOf(ListCatalog.MINE)) minePage(book)
    else ListsPage(screen.path.last(), listCatalog(lexico, book).entries(screen.path))
  }
  ListsPage(page, nav)
}

@Composable
internal fun UnfinishedStudiesRoute(lexico: Lexico, nav: Navigator) {
  val page = remember {
    val lists = allLists(lexico).filter { it.inProgress(System.currentTimeMillis()) }.sortedByDescending { it.percent }
    ListsPage("Estudios a medias", lists.map { CatalogEntry.Leaf(it) }, emptyNote = "Las listas que empieces a estudiar saldrán aquí.")
  }
  ListsPage(page, nav)
}

@Composable
internal fun CompletedStudiesRoute(lexico: Lexico, nav: Navigator) {
  val studies = remember { CompletedStudies(allLists(lexico)) }
  CompletedScreen(studies, nav::back) { nav.go(Screen.WordListDetail(it.id)) }
}

internal fun unfinishedStudies(lexico: Lexico): Int = allLists(lexico).count { it.inProgress(System.currentTimeMillis()) }

private fun allLists(lexico: Lexico): List<WordList> {
  val book = listBook(lexico.wordLists)
  return listCatalog(lexico, book).lists + book.all().filterNot { it.isDefault }
}

private fun minePage(book: ListBook): ListsPage =
  ListsPage(ListCatalog.MINE, book.all().filterNot { it.isDefault }.sortedByDescending { it.created }.map { CatalogEntry.Leaf(it) }, mine = true)

@Composable
private fun ListsPage(page: ListsPage, nav: Navigator) {
  ListsScreen(page, nav::back, { nav.replace(Screen.NewList) }, { nav.go(Screen.ListFolder(it.path)) }) { nav.go(Screen.WordListDetail(it.id)) }
}

@Composable
internal fun WordListDetailRoute(screen: Screen.WordListDetail, lexico: Lexico, nav: Navigator) {
  val book = remember { listBook(lexico.wordLists) }
  val list = rememberList(lexico, book, screen.id, nav) ?: return
  ListScreen(list, nav::back, { nav.go(Screen.ListStudy(list.id)) }, { nav.go(Screen.ListTest(list.id)) }) {
    book.delete(list.id)
    nav.back()
  }
}

@Composable
internal fun ListStudyRoute(screen: Screen.ListStudy, lexico: Lexico, nav: Navigator) {
  val book = remember { listBook(lexico.wordLists) }
  val list = rememberList(lexico, book, screen.id, nav) ?: return
  val lookup = LocalWordLookup.current
  WithLexicon(lexico) { lexicon ->
    StudyScreen(list, lexicon, nav::back, { book.save(list.copy(studied = it)) }) { lookup?.invoke(it) }
  }
}

@Composable
internal fun ListTestRoute(screen: Screen.ListTest, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  val book = remember { listBook(lexico.wordLists) }
  val list = rememberList(lexico, book, screen.id, nav) ?: return
  val scope = rememberCoroutineScope()
  WithLexicon(lexico) { lexicon ->
    val session = remember(lexicon) { TestSession(list, lexicon, engineHands(lexico), book, scope) }
    TestScreen(session, themes.board, nav::back)
  }
}

@Composable
private fun WithLexicon(lexico: Lexico, content: @Composable (Lexicon) -> Unit) = WithWords(lexico, content)

@Composable
private fun rememberList(lexico: Lexico, book: ListBook, id: String, nav: Navigator): WordList? {
  val list = remember(id) { if (ListCatalog.isDefault(id)) listCatalog(lexico, book).find(id) else book.all().find { it.id == id } }
  if (list == null) LaunchedEffect(id) { nav.back() }
  return list
}
