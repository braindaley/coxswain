package svenmeier.coxswain.compose.workout

import svenmeier.coxswain.gym.Measurement

/** Display latch only: keep the time/work average, but publish at most once per stroke. */
internal class StrokePaceValue {
    private var previous: Measurement? = null
    private var publishedAt = 0
    private var value = 0

    fun update(raw: Measurement, average: Int): Int {
        val before = previous
        val reset = before == null || raw.duration < before.duration ||
            raw.distance < before.distance || raw.strokes < before.strokes
        val newStroke = before != null && raw.strokes > before.strokes
        // Counterless devices still receive a bounded refresh instead of freezing forever.
        val timedFallback = raw.strokes == 0 && raw.duration - publishedAt >= 5
        if (reset || newStroke || timedFallback) {
            value = average
            publishedAt = raw.duration
        }
        previous = Measurement(raw)
        return value
    }
}
