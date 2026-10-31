package app.atemkraft.ui.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Klebende Überschriften in einer echten Liste (Eigentümer 2026-10-31: „beim Hochscrollen verliert
 * man die Kategorie“, „dann wechselt er nicht mehr“): hinunter, hinauf und wieder hinunter, in
 * Schritten wie mit dem Finger und in Sprüngen wie nach der Rückkehr von einer Unterseite. Oben
 * steht immer „Übungen“ und darunter die Gruppe, in der man liest – als Kopie mit Inhalt.
 */
@RunWith(RobolectricTestRunner::class)
class StickyHeadersScrollTest {
    @get:Rule
    val rule = createComposeRule()

    private val groups = listOf("Beruhigen", "Aktivieren", "Ausgleichen", "Atem schulen")
    private var listState = LazyListState()

    /** Index der Karte [card] in Gruppe [group]: „Übungen“, dann je Gruppe Kopf und sechs Karten. */
    private fun cardIndex(group: Int, card: Int) = 1 + group * (CARDS + 1) + 1 + card

    /** Schlüssel der sichtbaren Kopien oben (mit Inhalt, also Höhe), sortiert: „Übungen“ und höchstens eine Gruppe. */
    private fun pinned(): List<String> = rule.onAllNodes(SemanticsMatcher("Kopie") { node -> node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(COPY) == true })
        .fetchSemanticsNodes()
        .filter { it.boundsInRoot.height > 0f }
        .map { it.config[SemanticsProperties.TestTag].removePrefix(COPY) }
        .sorted()

    private fun expect(group: String) = assertEquals(listOf("head", group).sorted(), pinned())

    private fun jumpTo(group: Int, card: Int) {
        rule.runOnIdle { runBlocking { listState.scrollToItem(cardIndex(group, card)) } }
        rule.waitForIdle()
    }

    /** Scrollt in Schritten wie mit dem Finger; liegt oben eine Karte 1–4 einer Gruppe, klebt diese. */
    private fun stepThrough(delta: Float) {
        val seen = mutableSetOf<String>()
        repeat(STEPS) {
            rule.runOnIdle { runBlocking { listState.scrollBy(delta) } }
            rule.waitForIdle()
            val key = rule.runOnIdle { listState.layoutInfo.visibleItemsInfo.first().key as? String } ?: return@repeat
            val group = groups.firstOrNull { key.startsWith("$it#") } ?: return@repeat
            val card = key.substringAfter('#').toInt()
            if (card in 1..CARDS - 2) {
                expect(group)
                seen += group
            }
        }
        // Jede Gruppe kam in dieser Richtung dran.
        assertEquals(groups.toSet(), seen)
    }

    @Test
    fun pinnedGroupFollowsTheListDownUpAndDownAgain() {
        setList()
        rule.waitForIdle()
        assertEquals(emptyList<String>(), pinned())

        // Zwei Runden: hinunter, hinauf, in Schritten.
        repeat(2) {
            stepThrough(STEP)
            stepThrough(-STEP)
        }
        // Sprünge über mehrere Gruppen hinweg, in beide Richtungen.
        groups.indices.reversed().forEach {
            jumpTo(it, 2)
            expect(groups[it])
        }
        groups.indices.forEach {
            jumpTo(it, 2)
            expect(groups[it])
        }
    }

    @Test
    fun headerSticksBeforeItWasEverSeen() {
        // Zurück von einer Unterseite: Die Liste steht mitten in „Ausgleichen“, ihr Kopf war nie zu sehen.
        listState = LazyListState(firstVisibleItemIndex = cardIndex(2, 2))
        setList()
        rule.waitForIdle()
        expect("Ausgleichen")
    }

    private fun setList() {
        rule.setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                TabListScaffold(header = {}, listState = listState) {
                    stickyItem(Sticky("head", level = 1)) { Text("Übungen", Modifier.height(48.dp)) }
                    groups.forEach { group ->
                        stickyItem(Sticky(group, level = 2)) { Text(group, Modifier.height(32.dp)) }
                        items(CARDS, key = { "$group#$it" }) { Spacer(Modifier.fillMaxWidth().height(120.dp)) }
                    }
                }
            }
        }
    }

    private companion object {
        const val CARDS = 6
        const val STEP = 90f
        const val STEPS = 60
        val COPY = stickyCopyTag("")
    }
}
