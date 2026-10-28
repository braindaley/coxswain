package svenmeier.coxswain.compose.workout

import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import propoid.db.Repository
import svenmeier.coxswain.Gym
import svenmeier.coxswain.gym.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntervalRailTest {
    private fun program() = Program("Pyramid").apply {
        getSegment(0).setDistance(500)
        addSegment(Segment(Difficulty.REST).setDuration(120))
        addSegment(Segment(Difficulty.HARD).setDistance(1000))
    }
    private fun sample(seconds: Int, meters: Int, index: Int, start: Int) = Snapshot().apply {
        duration.set(seconds); distance.set(meters); intervalIndex.set(index); intervalStart.set(start)
        difficulty.set(if (index == 1) Difficulty.REST else Difficulty.MEDIUM)
    }
    private fun recording(p: Program) = Workout(p).apply { duration.set(540); distance.set(1520); status.set(WorkoutStatus.COMPLETED) }
    private fun samples() = listOf(sample(60,200,0,0), sample(120,500,0,0),
        sample(180,510,1,120),sample(240,520,1,120),sample(300,720,2,240),sample(540,1520,2,240))

    @Test fun recordedDistanceAndRestMarkersFollowTheirOwnIntervalBands() {
        val p = program()
        val replay = IntervalRailReplay(p, recording(p), samples())
        assertEquals(50f/495, replay.progressAt(60f), .0001f) // Actual slow first half, not average speed.
        assertEquals(125f/495, replay.progressAt(120f), .0001f)
        assertEquals(185f/495, replay.progressAt(180f), .0001f) // Halfway through rest despite almost no meters.
        assertEquals(245f/495, replay.progressAt(240f), .0001f)
        assertEquals(295f/495, replay.progressAt(300f), .0001f) // Recovery drift isn't second-interval progress.
        assertEquals(1f, replay.progressAt(540f), .0001f)
        assertEquals(1f, replay.progressAt(600f), .0001f)
        assertEquals(0f, replay.progressAt(0f), .0001f)
    }

    @Test fun timedMixedStrokeAndEnergyIntervalsUseTheirOwnTargets() {
        val p = Program("Mixed").apply {
            getSegment(0).setDuration(60)
            addSegment(Segment(Difficulty.REST).setDuration(30))
            addSegment(Segment(Difficulty.MEDIUM).setStrokes(20))
            addSegment(Segment(Difficulty.HARD).setEnergy(10))
        }
        val rows = listOf(sample(60,200,0,0), sample(90,210,1,60),
            sample(120,300,2,90), sample(150,400,2,90),sample(180,500,3,150),sample(210,600,3,150))
        rows.forEachIndexed { i,s -> s.strokes.set(listOf(20,20,30,40,50,60)[i]); s.energy.set(listOf(5,5,8,10,15,20)[i]) }
        val w = Workout(p).apply { duration.set(210); distance.set(600) }
        val replay = IntervalRailReplay(p,w,rows)
        assertEquals(30f/210,replay.progressAt(30f),.0001f)
        assertEquals(75f/210,replay.progressAt(75f),.0001f)
        assertEquals(120f/210,replay.progressAt(120f),.0001f)
        assertEquals(180f/210,replay.progressAt(180f),.0001f)
    }

    @Test fun legacyRecordingsAndMissingSamplesStillHaveUsableIntervalPositions() {
        val p = program()
        val rows = samples().map { it.apply { intervalIndex.set(-1); intervalStart.set(0) } }
        val replay = IntervalRailReplay(p,recording(p),rows)
        assertEquals(185f/495,replay.progressAt(180f),.0001f)
        assertEquals(295f/495,replay.progressAt(300f),.0001f)
        val estimated = IntervalRailReplay(p,recording(p),emptyList())
        assertEquals(.5f,estimated.progressAt(270f),.0001f)
    }

    @Test fun virtualPaceTargetIncludesRecoveryAndDifferentDistanceSteps() {
        val p = program() // At 2:30 /500m, first work ends at 150, rest at 270.
        assertEquals(125f/495,IntervalRailReplay.paceProgress(p,150,150f),.0001f)
        assertEquals(185f/495,IntervalRailReplay.paceProgress(p,150,210f),.0001f)
        assertEquals(245f/495,IntervalRailReplay.paceProgress(p,150,270f),.0001f)
        assertEquals(370f/495,IntervalRailReplay.paceProgress(p,150,420f),.0001f)
    }

    @Test fun regularAndPeteRacesUseSavedTransitionsWhileLiveRowFollowsItsOwnInterval() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("gym")
        val gym = Gym::class.java.getDeclaredConstructor(Context::class.java).apply { isAccessible = true }.newInstance(context)
        Gym::class.java.getDeclaredMethod("initialize").apply { isAccessible = true }.invoke(gym)
        val repository = Gym::class.java.getDeclaredField("repository").apply { isAccessible = true }.get(gym) as Repository
        try {
            val p = program()
            gym.mergeProgram(p)
            val best = recording(p)
            gym.mergeWorkout(best)
            samples().forEach { it.workout.set(best); repository.insert(it) }
            for (plan in listOf(false,true)) {
                if(plan) gym.startPlanSession(p,"enrollment",1,1,1,"ROW_AGAINST",0,0L,best)
                else gym.race(p,best)
                gym.onMeasured(Measurement().apply { duration=150;distance=500 })
                gym.onMeasured(Measurement().apply { duration=180;distance=510 })
                val index = p.getSegments().indexOf(gym.progress.segment)
                assertEquals(1,index)
                assertEquals(155f/495,IntervalRailReplay.position(p,index,gym.progress.completion()),.0001f)
                assertEquals(185f/495,gym.getPaceRailProgress(180),.0001f)
                // Pace/distance comparison remains a separate work-only measurement.
                assertEquals(500f,gym.getPaceDistanceAt(180),.0001f)
                gym.endEarly()
            }
        } finally { repository.close();context.deleteDatabase("gym") }
    }
}
