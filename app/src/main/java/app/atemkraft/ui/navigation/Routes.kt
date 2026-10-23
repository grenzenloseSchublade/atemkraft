package app.atemkraft.ui.navigation

import app.atemkraft.domain.PatternSpec
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation-Routen. Tabs: Atmen, Anlässe (Situationen), Eigene, Meditation, Lernen.
 * Push-Seiten mit festem Kopf: Detail, Muster-Editor („Eigenes Muster“), Einstellungen, Logbuch,
 * Glossar, Über, Atemlehre, Pranayama.
 */
@Serializable
object AtmenRoute

@Serializable
object SituationenRoute

/** Tab „Eigene“: „Neues Muster“ und die Liste „Meine Muster“. */
@Serializable
object EigeneRoute

@Serializable
object MeditationRoute

/** Tab „Lernen“: Kapitel Atemlehre, Pranayama und Begriffe (Nutzer 2026-10-23). */
@Serializable
object LernenRoute

/** Seite „Pranayama“ im Tab „Lernen“: Grundlagen und Techniken mit „Üben ›“. */
@Serializable
object PranayamaRoute

/** [focusMeditation]: vom Meditations-Tab geöffnet – springt direkt zur Meditations-Karte. */
@Serializable
data class SettingsRoute(val focusMeditation: Boolean = false)

/** Logbuch als Push-Seite, erreichbar aus Einstellungen › Daten. */
@Serializable
object LogbookRoute

@Serializable
object GlossaryRoute

@Serializable
object AboutRoute

/** [focus]: Kennung einer Muster-Karte (z. B. „langhana“) – Sprung aus der Detailseite, Karte aufgeklappt. */
@Serializable
data class AtemlehreRoute(val focus: String? = null)

@Serializable
data class DetailRoute(val exerciseId: String)

/**
 * Muster-Editor „Eigenes Muster“. Ohne Argumente ein neues Muster (Tab „Eigene“, Link im
 * Atmen-Tab); [fromDaily]: die Zahlen des Musters des Tages (Stift auf dessen Detailseite);
 * [patternId]: ein gespeichertes Muster bearbeiten (DB-Id, Stift auf seiner Detailseite).
 *
 * [daily] trägt die Zahlen genau des Tagesmusters, das die Detailseite zeigt ([of]): Die
 * Detailseite löst es einmal auf, über Mitternacht oder nach „Neu generieren“ wäre ein erneutes
 * `daily()` im Editor ein anderes Muster. Fehlt es (alte Route), gilt das aktuelle Tagesmuster.
 */
@Serializable
data class PatternEditorRoute(val patternId: Long? = null, val fromDaily: Boolean = false, val daily: String? = null) {
    /** Die mitgegebenen Zahlen des Tagesmusters; null, wenn keine oder unlesbare mitkamen. */
    fun dailySpec(): PatternSpec? {
        val parts = daily?.split(SEPARATOR) ?: return null
        if (parts.size != FIELDS) return null
        val numbers = parts.take(FIELDS - 1).map { it.toDoubleOrNull() ?: return null }
        val activating = parts.last().toBooleanStrictOrNull() ?: return null
        return PatternSpec(
            inhale = numbers[0],
            holdFull = numbers[1].takeIf { it > 0.0 },
            exhale = numbers[2],
            holdEmpty = numbers[3].takeIf { it > 0.0 },
            activating = activating,
        )
    }

    companion object {
        private const val SEPARATOR = ","
        private const val FIELDS = 5

        /** Editor mit den Zahlen von [spec], dem angezeigten Muster des Tages (Halten ohne = 0). */
        fun of(spec: PatternSpec) = PatternEditorRoute(
            fromDaily = true,
            daily = listOf(spec.inhale, spec.holdFull ?: 0.0, spec.exhale, spec.holdEmpty ?: 0.0, spec.activating)
                .joinToString(SEPARATOR),
        )
    }
}
