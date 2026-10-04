package svenmeier.coxswain.pete

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONObject
import svenmeier.coxswain.Gym
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.gym.WorkoutDefinition
import svenmeier.coxswain.gym.WorkoutStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.math.roundToInt

/** Enrollment state lives in backed-up preferences; workout identity lives with each workout. */
data class PetePlanState(
    val enrollmentId: String = "",
    val activeWeek: Int = 1,
    val activeAttempt: Int = 1,
    val weekStart: LocalDate? = null,
    val pendingRollover: Boolean = false,
    val finished: Boolean = false
) {
    val started get() = enrollmentId.isNotEmpty()
    val weekEnd get() = weekStart?.plusDays(6)
}

class PetePlanStore(context: Context, private val gym: Gym) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    val catalog: PetePlanCatalog = PetePlanCatalog.load(context.applicationContext)

    fun state(today: LocalDate = LocalDate.now(ZoneId.systemDefault())): PetePlanState {
        var value = read()
        if (!value.started || value.pendingRollover || value.finished) return value
        while (value.weekEnd != null && today.isAfter(value.weekEnd)) {
            if (requiredComplete(value) < 3) {
                value = value.copy(pendingRollover = true)
                break
            }
            value = if (value.activeWeek == 24) value.copy(finished = true)
            else value.copy(
                activeWeek = value.activeWeek + 1,
                activeAttempt = 1,
                weekStart = maxOf(value.weekStart!!.plusWeeks(1), calendarWeekStart(today))
            )
        }
        if (value != read()) save(value)
        return value
    }

    fun enroll(today: LocalDate = LocalDate.now(ZoneId.systemDefault())): PetePlanState {
        val existing = read()
        if (existing.started) return existing
        val value = PetePlanState(
            enrollmentId = UUID.randomUUID().toString(),
            weekStart = calendarWeekStart(today)
        )
        save(value)
        return value
    }

    /** Stop the current enrollment. Completed rows remain ordinary History entries. */
    fun stop() {
        preferences.edit().remove(KEY).apply()
    }

    fun resolveRollover(repeat: Boolean, today: LocalDate = LocalDate.now(ZoneId.systemDefault())): PetePlanState {
        val current = state(today)
        require(current.pendingRollover)
        val nextStart = maxOf(current.weekStart!!.plusWeeks(1), calendarWeekStart(today))
        val updated = if (repeat) current.copy(
            activeAttempt = current.activeAttempt + 1,
            weekStart = nextStart,
            pendingRollover = false
        ) else if (current.activeWeek == 24) current.copy(
            finished = true,
            pendingRollover = false
        ) else current.copy(
            activeWeek = current.activeWeek + 1,
            activeAttempt = 1,
            weekStart = nextStart,
            pendingRollover = false
        )
        save(updated)
        return updated
    }

    fun requiredComplete(state: PetePlanState = state()): Int = requiredComplete(state, state.activeWeek, state.activeAttempt)

    fun requiredComplete(state: PetePlanState, week: Int, attempt: Int): Int = planWorkouts(state)
        .asSequence().filter { it.planWeek.get() == week && it.planAttempt.get() == attempt }
        .mapNotNull { it.planSession.get() }.filter { it in 0..2 }.distinct().count()

    fun optionalComplete(state: PetePlanState = state()): Int = planWorkouts(state)
        .asSequence().filter { it.planWeek.get() == state.activeWeek && it.planAttempt.get() == state.activeAttempt }
        .mapNotNull { it.planSession.get() }.filter { it in 3..4 }.distinct().count()

    fun completedWorkout(state: PetePlanState, week: Int, attempt: Int, index: Int): Workout? =
        planWorkouts(state).filter {
            it.planWeek.get() == week && it.planAttempt.get() == attempt && it.planSession.get() == index
        }.minByOrNull { it.start.get() ?: Long.MAX_VALUE }

    fun latestCompleted(state: PetePlanState, week: Int, index: Int): Workout? =
        planWorkouts(state).filter {
            it.planWeek.get() == week && it.planSession.get() == index
        }.maxByOrNull { it.start.get() ?: 0L }

    fun latestAttempt(state: PetePlanState, week: Int): Int =
        if (week == state.activeWeek) state.activeAttempt else planWorkouts(state)
            .filter { it.planWeek.get() == week }.maxOfOrNull { it.planAttempt.get() ?: 1 } ?: 1

    fun planWorkouts(state: PetePlanState = state()): List<Workout> {
        if (!state.started) return emptyList()
        return gym.getAllWorkouts().list().filter {
            it.status.get() == WorkoutStatus.COMPLETED && it.planEnrollment.get() == state.enrollmentId &&
                (it.planWeek.get() ?: 0) in 1..24 && (it.planSession.get() ?: -1) in 0..4
        }
    }

    fun completedWeeks(state: PetePlanState = state()): Int = planWorkouts(state)
        .groupBy { it.planWeek.get() to it.planAttempt.get() }
        .filterValues { rows -> rows.mapNotNull { it.planSession.get() }.containsAll(listOf(0, 1, 2)) }
        .keys.mapNotNull { it.first }.distinct().count()

    fun weeklyTotals(state: PetePlanState = state(), week: Int): Pair<Int, Int> {
        val rows = planWorkouts(state).filter { it.planWeek.get() == week }
        return rows.sumOf { activeDistance(it) } to rows.sumOf { activeSeconds(it) }
    }

    fun allWeeklyTotals(state: PetePlanState = state()): List<Pair<Int, Int>> {
        val byWeek = planWorkouts(state).groupBy { it.planWeek.get() }
        return (1..24).map { week ->
            val rows = byWeek[week].orEmpty()
            rows.sumOf { activeDistance(it) } to rows.sumOf { activeSeconds(it) }
        }
    }

    fun activeSeconds(workout: Workout): Int {
        if ((workout.planActiveSeconds.get() ?: 0) > 0)
            return workout.planActiveSeconds.get() ?: 0
        val elapsed = (workout.duration.get() ?: 0).coerceAtLeast(0)
        if (workout.planEnrollment.get().isNullOrEmpty()) return elapsed
        val definition = runCatching { WorkoutDefinition.thaw(workout.programDefinition.get()) }.getOrNull()
        val rests = definition?.segments?.get()?.filter { it.difficulty.get() == Difficulty.REST }
            ?.sumOf { it.duration.get() ?: 0 } ?: 0
        return (elapsed - rests).coerceAtLeast(0)
    }

    fun activeDistance(workout: Workout): Int =
        if ((workout.planActiveSeconds.get() ?: 0) > 0)
            workout.planActiveDistance.get() ?: 0 else workout.distance.get() ?: 0

    fun activeStrokes(workout: Workout): Int =
        if ((workout.planActiveSeconds.get() ?: 0) > 0)
            workout.planActiveStrokes.get() ?: 0 else workout.strokes.get() ?: 0

    fun averageSplit(workout: Workout): Int? {
        val distance = activeDistance(workout)
        val seconds = activeSeconds(workout)
        if (distance <= 0 || seconds <= 0 || workout.status.get() != WorkoutStatus.COMPLETED) return null
        return (500.0 * seconds / distance).roundToInt().takeIf { it in 45..600 }
    }

    fun estimateSplit(): Int = preferences.getInt(ESTIMATE_KEY, 0).takeIf { it in 90..240 } ?: gym.getAllWorkouts().list()
        .filter { it.status.get() == WorkoutStatus.COMPLETED }
        .maxByOrNull { it.start.get() ?: 0L }
        ?.let(::averageSplit) ?: 150

    fun setEstimateSplit(secondsPer500m: Int) {
        require(secondsPer500m in 90..240)
        preferences.edit().putInt(ESTIMATE_KEY, secondsPer500m).apply()
    }

    private fun read(): PetePlanState {
        val json = preferences.getString(KEY, null) ?: return PetePlanState()
        return runCatching {
            val root = JSONObject(json)
            if (root.optInt("version") != 1) return@runCatching PetePlanState()
            PetePlanState(
                enrollmentId = root.optString("enrollmentId", ""),
                activeWeek = root.optInt("activeWeek", 1).coerceIn(1, 24),
                activeAttempt = root.optInt("activeAttempt", 1).coerceAtLeast(1),
                weekStart = root.optString("weekStart", "").takeIf(String::isNotEmpty)?.let(LocalDate::parse),
                pendingRollover = root.optBoolean("pendingRollover"),
                finished = root.optBoolean("finished")
            )
        }.getOrDefault(PetePlanState())
    }

    private fun save(value: PetePlanState) {
        val root = JSONObject().apply {
            put("version", 1)
            put("enrollmentId", value.enrollmentId)
            put("activeWeek", value.activeWeek)
            put("activeAttempt", value.activeAttempt)
            put("weekStart", value.weekStart?.toString() ?: "")
            put("pendingRollover", value.pendingRollover)
            put("finished", value.finished)
        }
        preferences.edit().putString(KEY, root.toString()).apply()
    }

    companion object {
        private const val KEY = "petes_plan_state_v1"
        private const val ESTIMATE_KEY = "petes_plan_estimated_split_seconds"
    }
}

private fun calendarWeekStart(today: LocalDate): LocalDate =
    today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY))
