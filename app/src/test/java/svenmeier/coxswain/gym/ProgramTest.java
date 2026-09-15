package svenmeier.coxswain.gym;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class ProgramTest {

    @Test
    public void constructorCreatesSingleDefaultSegment() {
        Program program = new Program("Test");

        assertEquals("Test", program.name.get());
        assertEquals(1, program.getSegmentsCount());
        assertEquals(1000, program.getSegment(0).getTarget());
    }

    @Test
    public void preservesFreeFormSegmentOrderAndTargets() {
        Program program = new Program("Intervals");
        program.getSegments().clear();
        program.addSegment(new Segment(Difficulty.HARD).setDuration(300));
        program.addSegment(new Segment(Difficulty.REST).setDuration(60));
        program.addSegment(new Segment(Difficulty.EASY).setDistance(2000));

        assertEquals(3, program.getSegmentsCount());
        assertEquals(Difficulty.HARD, program.getSegment(0).difficulty.get());
        assertEquals(300, program.getSegment(0).getTarget());
        assertEquals(Difficulty.REST, program.getSegment(1).difficulty.get());
        assertEquals(60, program.getSegment(1).getTarget());
        assertEquals(Difficulty.EASY, program.getSegment(2).difficulty.get());
        assertEquals(2000, program.getSegment(2).getTarget());
    }

    @Test
    public void duplicateSegmentCopiesValuesWithoutSharingInstance() {
        Program program = new Program("Intervals");
        Segment original = program.getSegment(0).setDuration(90).setPower(180);

        Segment duplicate = program.duplicateSegment(original);

        assertNotSame(original, duplicate);
        assertEquals(original.duration.get(), duplicate.duration.get());
        assertEquals(original.power.get(), duplicate.power.get());
    }
}
