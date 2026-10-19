package app.atemkraft.playback

import app.atemkraft.domain.BreathLevel
import app.atemkraft.domain.PhaseType

/** Ablauf-Zustand einer Atem-Session; der [SessionAudioCoordinator] und die UI folgen ihm. */
enum class SessionStatus { IDLE, PREPARING, RUNNING, PAUSED, WAITING_FOR_USER, FINISHED }

/** Audio-Info zum Phasenbeginn für den durchgehenden Ton (Typ + Dauer; open = ohne feste Dauer). */
data class PhaseAudio(val type: PhaseType, val durationMs: Long, val open: Boolean, val level: BreathLevel? = null)
