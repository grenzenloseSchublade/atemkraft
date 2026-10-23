package app.atemkraft.domain

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.CoroutineContext

/**
 * Scope-Politik der App (A-05): Jeder prozessweite Scope (`AppContainer.appScope`, die Abläufe,
 * Audio, Stimmen, Muster-Cache, Service) entsteht über [supervisedScope]. Ein unbehandelter Fehler
 * in einer seiner Coroutines – etwa ein voller Speicher beim Schreiben eines Lesezeichens – beendet
 * die App nicht: Der [SupervisorJob] hält die Geschwister am Leben, [UncaughtErrors.handler] fängt
 * den Fehler statt des Default-Handlers ab. Verschluckt wird er nicht: [UncaughtErrors.last] hält
 * die Art des letzten Fehlers, die Über-Seite zeigt sie als stillen Hinweis. Kein Logcat, keine
 * Meldung nach außen (SEC-PRIV-01, -03).
 */
object UncaughtErrors {
    private val _last = MutableStateFlow<String?>(null)

    /** Art (Klassenname) des letzten abgefangenen Fehlers; null = bisher keiner. Ohne Nachricht: Die kann Nutzerdaten enthalten. */
    val last: StateFlow<String?> = _last.asStateFlow()

    val handler = CoroutineExceptionHandler { _, error ->
        _last.value = error.javaClass.simpleName.ifEmpty { error.javaClass.name }
    }
}

/** Ein prozessweiter Scope nach der Politik oben: [SupervisorJob], [dispatcher], [UncaughtErrors.handler]. */
fun supervisedScope(dispatcher: CoroutineContext): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher + UncaughtErrors.handler)
