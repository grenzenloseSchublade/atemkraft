package app.atemkraft.ui

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation-Routen. Tabs: Atmen, Situationen, Eigene, Meditation. Push-Seiten mit
 * festem Kopf: Detail, Muster-Editor („Eigenes Muster“), Einstellungen, Logbuch, Glossar, Über,
 * Atemlehre.
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
 */
@Serializable
data class PatternEditorRoute(val patternId: Long? = null, val fromDaily: Boolean = false)
