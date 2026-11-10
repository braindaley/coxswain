package svenmeier.coxswain.compose.workout

/** Monotonic display clock; incoming packets may correct it down, never rewind it. */
internal class RestCountdown {
    private var lastTime: Long? = null
    private var lastReported: Int? = null
    private var remaining = 0L
    private var wasPaused = false

    fun update(reportedSeconds: Int, paused: Boolean, nowMillis: Long): Int {
        val previousTime = lastTime
        if (previousTime == null) remaining = reportedSeconds.coerceAtLeast(0) * 1000L
        else if (!wasPaused) remaining -= (nowMillis - previousTime).coerceAtLeast(0)
        if (lastReported != reportedSeconds) remaining = minOf(remaining, reportedSeconds.coerceAtLeast(0) * 1000L)
        lastTime = nowMillis
        lastReported = reportedSeconds
        wasPaused = paused
        return ((remaining.coerceAtLeast(0) + 999) / 1000).toInt()
    }
}
