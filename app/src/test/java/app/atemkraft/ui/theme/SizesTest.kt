package app.atemkraft.ui.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LAYOUT-04: Seit die Schrift Material-Standard ist, haben die Bedienelemente Material-3-Größe –
 * kein eigener Faktor. Wo M3 die Größe als Konstante veröffentlicht, wird dagegen geprüft; die
 * übrigen Tokens stehen auf den Werten der M3-Spezifikation. Die Tippfläche (A11Y-05) ist nie
 * kleiner als die sichtbare Fläche.
 */
class SizesTest {

    @Test
    fun `Bedienelemente haben Material-3-Groesse`() {
        assertEquals(ButtonDefaults.MinHeight, Sizes.ButtonHeight)
        assertEquals(FilterChipDefaults.Height, Sizes.ChipHeight)
        assertEquals(ButtonDefaults.IconSize, Sizes.IconInButton)
        assertEquals(40.dp, Sizes.IconButtonSize)
        assertEquals(24.dp, Sizes.IconDefault)
        // Icon 18 + Abstand 8 (privat in M3), den SegmentedButton auch ohne Icon reserviert.
        assertEquals(SegmentedButtonDefaults.IconSize + 8.dp, Sizes.SegmentIconReserve)
    }

    @Test
    fun `keine sichtbare Groesse ueber der 48-dp-Tippflaeche`() {
        assertEquals(48.dp, Dimens.MinTouchTarget)
        listOf(Sizes.ButtonHeight, Sizes.ChipHeight, Sizes.IconButtonSize, Sizes.IconDefault, Sizes.IconInButton, Sizes.IconSmall, Sizes.ProgressInline)
            .forEach { assertTrue("$it > ${Dimens.MinTouchTarget}", it <= Dimens.MinTouchTarget) }
    }
}
