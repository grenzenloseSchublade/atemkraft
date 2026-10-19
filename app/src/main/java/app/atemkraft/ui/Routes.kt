package app.atemkraft.ui

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation-Routen. Tabs: Atmen, Situationen, Meditation, Logbuch. Push-Seiten mit
 * festem Kopf: Detail, Einstellungen, Glossar, Über, Atemlehre.
 */
@Serializable
object AtmenRoute

@Serializable
object SituationenRoute

@Serializable
object LogbuchRoute

@Serializable
object MeditationRoute

/** [focusMeditation]: vom Meditations-Tab geöffnet – springt direkt zur Meditations-Karte. */
@Serializable
data class SettingsRoute(val focusMeditation: Boolean = false)

@Serializable
object GlossaryRoute

@Serializable
object AboutRoute

/** [focus]: Kennung einer Muster-Karte (z. B. „langhana“) – Sprung aus der Detailseite, Karte aufgeklappt. */
@Serializable
data class AtemlehreRoute(val focus: String? = null)

@Serializable
data class DetailRoute(val exerciseId: String)
