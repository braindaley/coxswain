package svenmeier.coxswain

import svenmeier.coxswain.gym.Workout

/** Older rows can be viewed with current settings; recorded profiles always take precedence. */
internal data class WorkoutZoneProfiles(val heart: HeartRateZones?, val output: PerformanceZones?,
                                       val currentHeart: Boolean, val currentOutput: Boolean) {
    companion object {
        fun resolve(workout: Workout, currentHeart: HeartRateZones?, currentOutput: PerformanceZones?): WorkoutZoneProfiles {
            val storedHeart = workout.heartRateZones.get()
            val storedOutput = workout.performanceZones.get()
            // A malformed or deliberately partial stored profile must not be silently replaced.
            val heart = if (storedHeart.isNullOrBlank()) currentHeart else HeartRateZones.decode(storedHeart)
            val output = if (storedOutput.isNullOrBlank()) currentOutput else PerformanceZones.decode(storedOutput)
            return WorkoutZoneProfiles(heart, output, storedHeart.isNullOrBlank() && heart != null,
                storedOutput.isNullOrBlank() && output != null)
        }
    }
}
