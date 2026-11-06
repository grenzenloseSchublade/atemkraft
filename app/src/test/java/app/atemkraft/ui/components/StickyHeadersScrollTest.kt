package app.atemkraft.ui.components

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.atemkraft.ui.theme.Motion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs

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
    private fun pinned(): List<String> = copies()
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

    /**
     * Aufklappbarer Abschnitt in einer Spalte (Eigentümer 2026-11-06): Offen klebt sein Kopf, und die
     * Kopie zeigt nur die Titelzeile – ohne den Rand darüber und ohne den Teaser darunter; der nächste
     * Kopf schiebt sie bündig hinaus. Ein Tipp auf die Kopie klappt zu, ohne dass der Kopf springt –
     * das Original steht danach, wo die Kopie stand. Zu klebt er nicht.
     */
    @Test
    fun openSectionSticksAndItsCopyClosesWithoutJump() {
        val scroll = ScrollState(0)
        var open by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                PushScaffold(onBack = {}, scrollState = scroll) {
                    CollapsibleSection(title = "Vorher", sticky = Sticky("before", level = 1)) { Spacer(Modifier.height(100.dp)) }
                    CollapsibleSection(title = "Offen", expanded = open, onExpandedChange = { open = it }, teaser = TEASER) {
                        Spacer(Modifier.fillMaxWidth().height(1500.dp).testTag(BODY))
                    }
                    CollapsibleSection(title = "Danach", expanded = false) {}
                    Spacer(Modifier.height(2000.dp))
                }
            }
        }
        val header = rule.onNode(hasText("Offen") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        // Titelzeile als Tippfläche: „Danach“ ist derselbe Kopf ohne Teaser (Lage und Größe ungeschnitten).
        val next = rule.onNode(hasText("Danach") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        val titleLine = next.fetchSemanticsNode().size.height.toFloat()
        rule.runOnIdle { runBlocking { scroll.scrollTo(1000) } }
        rule.waitForIdle()

        // Offen: genau eine Kopie, so hoch wie die Titelzeile – kein Rand über ihr, kein Teaser darunter.
        val copies = copies()
        assertEquals(1, copies.size)
        val copy = copies.single().boundsInRoot
        assertEquals(titleLine, copy.height, 1f)

        // „Danach“ schiebt die Kopie bündig hinaus: Ihre Unterkante liegt auf seiner Tippfläche.
        val pushBy = next.fetchSemanticsNode().positionInRoot.y - (copy.top + copy.height / 2)
        rule.runOnIdle { runBlocking { scroll.scrollBy(pushBy) } }
        rule.waitForIdle()
        val pushed = copies().single()
        assertEquals(next.fetchSemanticsNode().positionInRoot.y, pushed.positionInRoot.y + pushed.size.height, 1f)
        rule.runOnIdle { runBlocking { scroll.scrollTo(1000) } }
        rule.waitForIdle()

        // Tipp auf die Kopie: zu, und der echte Kopf steht genau dort, wo die Kopie stand. Bild für
        // Bild: Der Inhalt bleibt erst stehen (kein Loch unter dem Kopf), und „Danach“ steht nach
        // der Zeit für eine Bildschirmhöhe still – gleitet wird nur der sichtbare Teil (A54: holprig).
        rule.mainClock.autoAdvance = false
        rule.onNode(isCopy).performClick()
        repeat(FRAMES) { if (open) rule.mainClock.advanceTimeByFrame() }
        rule.mainClock.advanceTimeByFrame()
        val firstFrame = rule.onAllNodes(hasTestTag(BODY)).fetchSemanticsNodes().sumOf { it.boundsInRoot.height.toDouble() }
        val screen = with(rule.density) { 640.dp.toPx() }
        val oneScreen = Motion.Disclosure.vectorize(IntSize.VectorConverter)
            .getDurationNanos(AnimationVector2D(0f, screen), AnimationVector2D(0f, 0f), AnimationVector2D(0f, 0f)) / 1_000_000
        rule.mainClock.advanceTimeBy(oneScreen + 2 * FRAME_MS)
        val nextAfter = next.fetchSemanticsNode().positionInRoot.y
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertTrue(firstFrame > 0 && abs(nextAfter - next.fetchSemanticsNode().positionInRoot.y) <= 1f)
        assertFalse(open)
        assertEquals(copy.top, header.fetchSemanticsNode().boundsInRoot.top, 1f)
        assertEquals(0, copies().size)

        // Zu klebt er nicht, auch wenn er oben hinausscrollt.
        rule.runOnIdle { runBlocking { scroll.scrollBy(100f) } }
        rule.waitForIdle()
        assertEquals(0, copies().size)
    }

    private val isCopy = SemanticsMatcher("Kopie") { node -> node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(COPY) == true }

    /** Sichtbare Kopien oben (mit Höhe). */
    private fun copies() = rule.onAllNodes(isCopy)
        .fetchSemanticsNodes()
        .filter { it.boundsInRoot.height > 0f }

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
        const val TEASER = "Ein Teaser über zwei Zeilen, der als gelesene Einleitung nicht oben mitklebt, nur der Titel."
        val COPY = stickyCopyTag("")
        const val BODY = "body"
        const val FRAMES = 10
        const val FRAME_MS = 16L
    }
}
