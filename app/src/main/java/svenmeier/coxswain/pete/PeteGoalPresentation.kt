package svenmeier.coxswain.pete

import svenmeier.coxswain.gym.WorkoutDefinition
import svenmeier.coxswain.gym.Workout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class PeteGoalPresentation(val title: String, val value: String, val detail: String, val startLabel: String)
private fun splitClock(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

/** Describe the metric being coached separately from an actual recorded race. */
internal fun presentPeteGoal(goal: PeteGoal, sourceLabel: (Workout) -> String,
                             sourceSplit: (Workout) -> Int?): PeteGoalPresentation? = when (goal) {
    is PeteGoal.Speed -> {
        val baseline = sourceSplit(goal.source)
        val difference = baseline?.let { goal.splitSeconds - it }
        val comparison = when {
            difference == 0 -> "Match its average split."
            difference != null && difference < 0 -> "${-difference} seconds faster per 500 m than its average split."
            difference != null -> "$difference seconds slower per 500 m than its average split."
            else -> "Hold this steady split."
        }
        PeteGoalPresentation("SPLIT TARGET", "Hold ${splitClock(goal.splitSeconds)} /500 m",
            (if (goal.firstPieces > 0) "First ${goal.firstPieces} work intervals only. Then follow the coaching note.\n" else "") +
                "$comparison\nFrom ${sourceLabel(goal.source)}", "Start with pace target")
    }
    is PeteGoal.StrokeRateCap -> PeteGoalPresentation("STROKE-RATE CAP", "Stay at or below ${goal.spm} SPM",
        listOfNotNull(goal.description.takeIf { it.isNotBlank() }, goal.source?.let { "From ${sourceLabel(it)}" })
            .joinToString("\n").ifBlank { "Keep the recovery relaxed and focus on technique." }, "Start with stroke-rate target")
    is PeteGoal.StrokeRate -> PeteGoalPresentation("STROKE-RATE TARGET", "Hold ${goal.spm} SPM",
        "Strokes per minute. Keep a steady rhythm and focus on technique.", "Start with stroke-rate target")
    is PeteGoal.RowAgainst -> PeteGoalPresentation("RACE PREVIOUS ROW",
        if (WorkoutDefinition.ranksByDistance(WorkoutDefinition.thaw(goal.source.programDefinition.get())))
            "Row farther in the same time" else "Beat your previous time",
        "${sourceLabel(goal.source)}\nReplay its actual pacing, including interval changes.", "Race previous row")
    is PeteGoal.Reference -> PeteGoalPresentation("SPLIT REFERENCE", "Reference: ${splitClock(goal.splitSeconds)} /500 m",
        "${goal.description}\nFrom ${sourceLabel(goal.source)}", "Start with pacing reference")
    is PeteGoal.Unavailable -> PeteGoalPresentation("NO TARGET YET", "No completed reference available",
        goal.description, "Start without target")
    PeteGoal.None -> null
}

internal fun peteLiveInstruction(kind: String?, value: Int, activeSpeed: Int, resting: Boolean,
                                 timedRace: Boolean): String? {
    if (resting) return null
    return when (kind) {
        "SPEED" -> "Hold ${splitClock(value)} /500 m"
        "SPEED_PHASE" -> if (activeSpeed > 0) "Hold ${splitClock(value)} /500 m"
            else "Pace target finished · Follow the coaching note"
        "RATE_CAP" -> "Stay at or below $value SPM"
        "RATE" -> "Hold $value SPM"
        "REFERENCE" -> "Pace reference · ${splitClock(value)} /500 m"
        "ROW_AGAINST" -> if (timedRace) "Beat your previous distance" else "Beat your previous time"
        "UNAVAILABLE" -> "Row by feel · No completed reference yet"
        else -> null
    }
}

internal fun peteSourceDate(start: Long): String = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(start))
