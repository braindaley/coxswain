package svenmeier.coxswain.pete

import android.content.Context
import org.json.JSONObject
import svenmeier.coxswain.Gym
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.gym.WorkoutDefinition
import svenmeier.coxswain.gym.WorkoutStatus
import kotlin.math.roundToInt

sealed class PeteGoal {
    data object None : PeteGoal()
    data class Speed(val splitSeconds: Int, val source: Workout, val firstPieces: Int = 0) : PeteGoal()
    data class StrokeRateCap(val spm: Int, val source: Workout? = null, val description: String = "") : PeteGoal()
    data class StrokeRate(val spm: Int) : PeteGoal()
    data class RowAgainst(val source: Workout) : PeteGoal()
    data class Reference(val splitSeconds: Int, val source: Workout, val description: String) : PeteGoal()
    data class Unavailable(val description: String) : PeteGoal()

    val kind: String get() = when (this) {
        is Speed -> if (firstPieces > 0) "SPEED_PHASE" else "SPEED"
        is StrokeRateCap -> "RATE_CAP"
        is StrokeRate -> "RATE"
        is RowAgainst -> "ROW_AGAINST"
        is Reference -> "REFERENCE"
        is Unavailable -> "UNAVAILABLE"
        None -> "NONE"
    }
    val value: Int get() = when (this) {
        is Speed -> splitSeconds
        is StrokeRateCap -> spm
        is StrokeRate -> spm
        is Reference -> splitSeconds
        else -> 0
    }
    val sourceWorkout: Workout? get() = when (this) {
        is Speed -> source
        is StrokeRateCap -> source
        is RowAgainst -> source
        is Reference -> source
        else -> null
    }
}

/** Rules were authored once from the coaching notes; runtime never tries to parse prose. */
class PeteGoalResolver private constructor(
    private val rules: List<List<JSONObject>>,
    private val store: PetePlanStore,
    private val gym: Gym
) {
    fun resolve(session: PeteSession, state: PetePlanState = store.state()): PeteGoal {
        val rule = rules[session.week - 1][session.index]
        return resolveRule(rule, state)
    }

    private fun resolveRule(rule: JSONObject, state: PetePlanState): PeteGoal {
        val kind = rule.getString("kind")
        if (kind == "NONE") return PeteGoal.None
        if (kind == "RATE_CAP") return PeteGoal.StrokeRateCap(rule.getInt("value"), description = rule.optString("description", ""))
        if (kind == "RATE") return PeteGoal.StrokeRate(rule.getInt("value"))
        val source = chooseSource(rule, state)
            ?: return rule.optJSONObject("fallback")?.let { resolveRule(it, state) }
                ?: PeteGoal.Unavailable("Complete the referenced row to calculate this target.")
        if (kind == "ROW_AGAINST") return PeteGoal.RowAgainst(source)
        if (kind == "RATE_ADAPT") {
            val active = store.activeSeconds(source)
            val priorRate = if (active > 0) (store.activeStrokes(source) * 60.0 / active).roundToInt() else 0
            if (priorRate <= 0) return PeteGoal.Unavailable("A completed stroke count is needed for this rate target.")
            val cap = if (priorRate > rule.getInt("value")) priorRate - 1 else rule.getInt("value")
            return PeteGoal.StrokeRateCap(cap, source, "Ease down from $priorRate SPM; work toward 24 SPM over time.")
        }
        val baseline = if (rule.optString("mode") == "BEST_DISTANCE")
            (gym.getWorkoutTimeAtDistance(source, rule.getInt("value")) * 500 / rule.getInt("value")).roundToInt()
        else store.averageSplit(source)
            ?: return PeteGoal.Unavailable("The earlier row has no usable active-time split.")
        if (kind == "REFERENCE") return PeteGoal.Reference(baseline, source, rule.optString("description", "Use this result as your pacing reference."))
        val target = baseline + rule.optInt("offsetSeconds")
        if (target !in 45..600) return PeteGoal.Unavailable("The calculated split is outside a valid rowing range.")
        return PeteGoal.Speed(target, source, rule.optInt("firstPieces"))
    }

    private fun chooseSource(rule: JSONObject, state: PetePlanState): Workout? {
        if (rule.optString("mode") == "BEST_DURATION") {
            val seconds = rule.getInt("value")
            return gym.getAllWorkouts().list().filter { workout ->
                workout.status.get() == WorkoutStatus.COMPLETED && (workout.distance.get() ?: 0) > 0 &&
                    runCatching {
                        val program = WorkoutDefinition.thaw(workout.programDefinition.get())
                        program != null && program.getSegmentsCount() == 1 && program.getSegment(0).duration.get() == seconds
                    }.getOrDefault(false)
            }.maxByOrNull { store.activeDistance(it) }
        }
        if (rule.optString("mode") == "BEST_DISTANCE") {
            val meters = rule.optInt("value")
            return gym.getAllWorkouts().list().filter { workout ->
                workout.status.get() == WorkoutStatus.COMPLETED && (workout.distance.get() ?: 0) >= meters &&
                    store.activeSeconds(workout) > 0 &&
                    runCatching {
                        val program = WorkoutDefinition.thaw(workout.programDefinition.get())
                        program != null && program.getSegmentsCount() == 1 && program.getSegment(0).distance.get() == meters
                    }.getOrDefault(false)
            }.minByOrNull { gym.getWorkoutTimeAtDistance(it, meters) }
        }
        val refs = rule.optJSONArray("sources") ?: return null
        val candidates = (0 until refs.length()).mapNotNull { i ->
            val source = refs.getJSONObject(i)
            store.latestCompleted(state, source.getInt("week"), source.getInt("session"))
        }
        if (candidates.isEmpty()) return null
        return if (rule.optString("mode") == "FASTEST") candidates.minByOrNull { store.averageSplit(it) ?: Int.MAX_VALUE }
            else candidates.first()
    }

    companion object {
        fun load(context: Context, store: PetePlanStore, gym: Gym): PeteGoalResolver = parse(
            context.assets.open("petes_plan_rules.json").bufferedReader().use { it.readText() }, store, gym
        )

        fun parse(json: String, store: PetePlanStore, gym: Gym): PeteGoalResolver {
            val root = JSONObject(json)
            require(root.getInt("version") == 1)
            val source = root.getJSONArray("weeks")
            require(source.length() == 24)
            val rules = (0 until source.length()).map { w ->
                val week = source.getJSONArray(w)
                require(week.length() == 5)
                (0 until week.length()).map { week.getJSONObject(it) }
            }
            return PeteGoalResolver(rules, store, gym)
        }
    }
}
