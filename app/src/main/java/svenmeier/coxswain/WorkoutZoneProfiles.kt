package svenmeier.coxswain

import svenmeier.coxswain.gym.Workout

/** Older rows can be viewed with current settings; recorded profiles always take precedence. */
internal data class WorkoutZoneProfiles(val heart: HeartRateZones?, val output: PerformanceZones?,
                                       val currentHeart: Boolean, val currentOutput: Boolean, val currentCadence: Boolean = false) {
    companion object {
        fun resolve(workout: Workout, currentHeart: HeartRateZones?, currentOutput: PerformanceZones?): WorkoutZoneProfiles {
            val storedHeart = workout.heartRateZones.get()
            val storedOutput = workout.performanceZones.get()
            // A malformed or deliberately partial stored profile must not be silently replaced.
            val heart = if (storedHeart.isNullOrBlank()) currentHeart else HeartRateZones.decode(storedHeart)
            var output = if (storedOutput.isNullOrBlank()) currentOutput else PerformanceZones.decode(storedOutput)
            // Old profiles predate cadence. Explicit null in new profiles means cadence was disabled.
            val currentCadence = output != null && !storedOutput.isNullOrBlank() &&
                runCatching { !org.json.JSONObject(storedOutput).has("cadence") }.getOrDefault(false) && currentOutput?.cadence != null
            if (currentCadence) output = output?.copy(cadence = currentOutput?.cadence)
            return WorkoutZoneProfiles(heart, output, storedHeart.isNullOrBlank() && heart != null,
                storedOutput.isNullOrBlank() && output != null, currentCadence)
        }
    }
}
