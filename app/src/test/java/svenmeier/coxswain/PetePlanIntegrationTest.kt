package svenmeier.coxswain

import android.content.Context
import androidx.preference.PreferenceManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import propoid.db.Repository
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.Segment
import svenmeier.coxswain.gym.WorkoutStatus
import svenmeier.coxswain.pete.PetePlanCatalog
import svenmeier.coxswain.pete.PeteGoal
import svenmeier.coxswain.pete.PeteGoalResolver
import svenmeier.coxswain.pete.PetePlanStore
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PetePlanIntegrationTest {
    private lateinit var context: Context
    private lateinit var gym: Gym

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("gym")
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove("petes_plan_state_v1").commit()
        val constructor = Gym::class.java.getDeclaredConstructor(Context::class.java)
        constructor.isAccessible = true
        gym = constructor.newInstance(context)
        gym.initialize()
    }

    @After fun tearDown() {
        val field = Gym::class.java.getDeclaredField("repository")
        field.isAccessible = true
        (field.get(gym) as Repository).close()
        context.deleteDatabase("gym")
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove("petes_plan_state_v1").commit()
    }

    @Test fun ordinaryIntervalPaceEstimateExcludesRest() {
        val store = PetePlanStore(context, gym)
        val program = Program("Ordinary interval")
        program.getSegment(0).setDuration(60)
        program.addSegment(Segment(Difficulty.REST).setDuration(30))
        program.addSegment(Segment(Difficulty.MEDIUM).setDuration(60))
        gym.select(program)
        gym.onMeasured(measurement(60, 200, 24))
        gym.onMeasured(measurement(90, 220, 26))
        gym.onMeasured(measurement(150, 420, 50))
        val row = gym.complete()
        assertEquals(120, store.activeSeconds(row))
        assertEquals(400, store.activeDistance(row))
        assertEquals(150, store.averageSplit(row))
    }

    @Test fun catalogIncludesAllWeeksAndBuildsFullIntervalWithRest() {
        val catalog = PetePlanCatalog.load(context)
        assertEquals(24, catalog.weeks.size)
        assertTrue(catalog.weeks.all { week -> week.size == 5 && week.take(3).all { it.required } && week.drop(3).all { it.optional } })
        val session = catalog.session(1, 1)
        assertEquals(6, session.pieces)
        assertEquals(500, session.amount)
        assertEquals(120, session.restSeconds)
        assertEquals(25, session.estimatedMinutes(150))
        val program = session.program()
        assertEquals(11, program.segmentsCount)
        assertEquals(500, program.getSegment(0).distance.get())
        assertEquals(Difficulty.REST, program.getSegment(1).difficulty.get())
        assertEquals(120, program.getSegment(1).duration.get())
        assertEquals(500, program.getSegment(10).distance.get())
    }

    @Test fun completedIntervalExcludesRestAndRepeatClearsRequiredChecklist() {
        val store = PetePlanStore(context, gym)
        val saturday = LocalDate.of(2026, 10, 10)
        val state = store.enroll(saturday)
        assertEquals(LocalDate.of(2026, 10, 4), state.weekStart)

        val program = Program("Short interval")
        program.getSegments().clear()
        program.addSegment(Segment(Difficulty.MEDIUM).setDistance(100))
        program.addSegment(Segment(Difficulty.REST).setDuration(10))
        program.addSegment(Segment(Difficulty.MEDIUM).setDistance(100))
        gym.startPlanSession(program, state.enrollmentId, 1, 1, 0, "NONE", 0, 0L, null)
        gym.onMeasured(measurement(1, 10, 1))
        gym.onMeasured(measurement(10, 100, 10))
        gym.onMeasured(measurement(20, 120, 12))
        gym.onMeasured(measurement(30, 220, 22))
        val workout = gym.complete()
        assertEquals(WorkoutStatus.COMPLETED, workout.status.get())
        assertEquals(20, store.activeSeconds(workout))
        assertEquals(200, store.activeDistance(workout))
        assertEquals(20, store.activeStrokes(workout))
        assertEquals(50, store.averageSplit(workout))
        assertEquals(1, store.requiredComplete(state))
        assertNotNull(store.completedWorkout(state, 1, 1, 0))
        assertEquals(200 to 20, store.weeklyTotals(state, 1))

        val pending = store.state(LocalDate.of(2026, 10, 11))
        assertTrue(pending.pendingRollover)
        val repeat = store.resolveRollover(true, LocalDate.of(2026, 10, 11))
        assertEquals(1, repeat.activeWeek)
        assertEquals(2, repeat.activeAttempt)
        assertEquals(0, store.requiredComplete(repeat))
        assertNotNull(store.completedWorkout(repeat, 1, 1, 0))
        assertNull(store.completedWorkout(repeat, 1, 2, 0))
    }

    @Test fun endedEarlyRowNeverCreditsAPlanSession() {
        val store = PetePlanStore(context, gym)
        val state = store.enroll(LocalDate.of(2026, 10, 4))
        val program = Program.meters("Incomplete", 5000, Difficulty.MEDIUM)
        gym.startPlanSession(program, state.enrollmentId, 1, 1, 0, "NONE", 0, 0L, null)
        gym.onMeasured(measurement(30, 500, 60))
        val workout = gym.endEarly()
        assertEquals(WorkoutStatus.ENDED_EARLY, workout.status.get())
        assertEquals(0, store.requiredComplete(state))
        assertEquals(0 to 0, store.weeklyTotals(state, 1))
    }

    @Test fun namedPaceTargetUsesOnlyCompletedReferencedRow() {
        val store = PetePlanStore(context, gym)
        val state = store.enroll(LocalDate.of(2026, 10, 4))
        val catalog = PetePlanCatalog.load(context)
        val resolver = PeteGoalResolver.load(context, store, gym)
        val session = catalog.session(1, 2)
        assertTrue(resolver.resolve(session, state) is PeteGoal.Unavailable)

        val program = Program.meters("First 5K", 5000, Difficulty.MEDIUM)
        gym.startPlanSession(program, state.enrollmentId, 1, 1, 0, "NONE", 0, 0L, null)
        gym.onMeasured(measurement(1500, 5000, 600))
        val source = gym.complete()
        val goal = resolver.resolve(session, state) as PeteGoal.Speed
        assertEquals(150, goal.splitSeconds)
        assertEquals(source.start.get(), goal.source.start.get())
        assertEquals(333, session.program(goal).getSegment(0).speed.get())
    }

    @Test fun stoppingPlanClearsParticipationAndCreatesFreshEnrollment() {
        val store = PetePlanStore(context, gym)
        val state = store.enroll(LocalDate.of(2026, 10, 4))
        gym.startPlanSession(Program.meters("Plan row", 5000, Difficulty.MEDIUM), state.enrollmentId,
            1, 1, 0, "NONE", 0, 0L, null)
        gym.onMeasured(measurement(1500, 5000, 600))
        val completed = gym.complete()
        store.stop()
        assertFalse(store.state().started)
        assertTrue(store.planWorkouts(store.state()).isEmpty())
        val newEnrollment = store.enroll(LocalDate.of(2026, 10, 5))
        assertTrue(newEnrollment.started)
        assertNotEquals(state.enrollmentId, newEnrollment.enrollmentId)
        assertTrue(store.planWorkouts(newEnrollment).isEmpty())
        assertTrue(gym.getAllWorkouts().list().any { it.start.get() == completed.start.get() })
    }

    @Test fun coachingUsesExplicitOptionalFallbackAndCorrectSixThousandReference() {
        val store = PetePlanStore(context, gym)
        val state = store.enroll(LocalDate.of(2026, 10, 4))
        val resolver = PeteGoalResolver.load(context, store, gym)
        gym.startPlanSession(Program.minutes("20 minutes", 20, Difficulty.MEDIUM), state.enrollmentId,
            2, 1, 3, "NONE", 0, 0L, null)
        gym.onMeasured(measurement(1200, 4000, 480))
        gym.complete()
        assertEquals(150, (resolver.resolve(store.catalog.session(2, 4), state) as PeteGoal.Speed).splitSeconds)

        gym.startPlanSession(Program.meters("Optional 6K", 6000, Difficulty.MEDIUM), state.enrollmentId,
            4, 1, 3, "NONE", 0, 0L, null)
        gym.onMeasured(measurement(1800, 6000, 720))
        val reference = gym.complete()
        val target = resolver.resolve(store.catalog.session(4, 4), state) as PeteGoal.Speed
        assertEquals(149, target.splitSeconds)
        assertEquals(reference.start.get(), target.source.start.get())
    }

    @Test fun bestTimedReferenceExcludesIncompleteRows() {
        val store = PetePlanStore(context, gym)
        val state = store.enroll(LocalDate.of(2026, 10, 4))
        gym.select(Program.minutes("30 minutes", 30, Difficulty.MEDIUM))
        gym.onMeasured(measurement(1800, 6000, 720))
        val completed = gym.complete()
        gym.select(Program.minutes("Incomplete 30 minutes", 30, Difficulty.MEDIUM))
        gym.onMeasured(measurement(1700, 6500, 680))
        gym.endEarly()
        val resolver = PeteGoalResolver.load(context, store, gym)
        val target = resolver.resolve(store.catalog.session(23, 2), state) as PeteGoal.Reference
        assertEquals(150, target.splitSeconds)
        assertEquals(completed.start.get(), target.source.start.get())
    }

    @Test fun bestDistanceReferenceAcceptsOvershootAndUsesTimeAtTarget() {
        val store = PetePlanStore(context, gym)
        val state = store.enroll(LocalDate.of(2026, 10, 4))
        gym.select(Program.meters("10K overshoot", 10000, Difficulty.MEDIUM))
        gym.onMeasured(measurement(2900, 9900, 1160))
        gym.onMeasured(measurement(3000, 10003, 1200))
        val completed = gym.complete()
        val expected = kotlin.math.round(gym.getWorkoutTimeAtDistance(completed, 10000) * 500 / 10000).toInt()
        val resolver = PeteGoalResolver.load(context, store, gym)
        val target = resolver.resolve(store.catalog.session(20, 2), state) as PeteGoal.Speed
        assertEquals(expected, target.splitSeconds)
        assertEquals(completed.start.get(), target.source.start.get())
        assertTrue(gym.getWorkoutTimeAtDistance(completed, 10000) < 3000f)
    }

    private fun measurement(seconds: Int, meters: Int, strokes: Int): Measurement = Measurement().apply {
        duration = seconds
        distance = meters
        this.strokes = strokes
        strokeRate = 24
    }
}
