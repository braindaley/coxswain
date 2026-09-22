package svenmeier.coxswain

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.Segment
import svenmeier.coxswain.view.Utils

class ProgramDraftTest {

    @Test
    fun draftProgramModificationsDoNotMutateOriginalUntilSaved() {
        val original = Program("Intervals").apply {
            segments.get().clear()
            addSegment(Segment(Difficulty.HARD).setDistance(1000))
        }

        // Create draft clone
        val draft = Program(original.name.get() ?: "").apply {
            segments.get().clear()
            for (s in original.segments.get()) {
                segments.get().add(s.duplicate())
            }
        }

        // Modify draft
        draft.name.set("New Intervals")
        draft.segments.get()[0].setStrokeRate(24)
        draft.addSegment(Segment(Difficulty.EASY).setDuration(120))

        // Verify original is untouched
        assertEquals("Intervals", original.name.get())
        assertEquals(1, original.segments.get().size)
        assertEquals(0, original.segments.get()[0].strokeRate.get().toInt())

        // Save draft to original
        original.name.set(draft.name.get())
        original.segments.get().clear()
        for (s in draft.segments.get()) {
            original.segments.get().add(s.duplicate())
        }

        // Verify original updated
        assertEquals("New Intervals", original.name.get())
        assertEquals(2, original.segments.get().size)
        assertEquals(24, original.segments.get()[0].strokeRate.get().toInt())
    }

    @Test
    fun getCallbackReturnsNullWhenNotPresent() {
        val callback = Utils.getCallback(null as Activity?, String::class.java)
        assertNull(callback)
    }
}
