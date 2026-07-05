package app.atemkraft.ui

import kotlinx.serialization.Serializable

/** Type-safe Navigation-Routen. Top-Level: Atmen, Situationen, Logbuch. Push: Detail, Settings. */
@Serializable
object AtmenRoute

@Serializable
object SituationenRoute

@Serializable
object LogbuchRoute

@Serializable
object SettingsRoute

@Serializable
object GlossaryRoute

@Serializable
object AboutRoute

@Serializable
data class DetailRoute(val exerciseId: String)
