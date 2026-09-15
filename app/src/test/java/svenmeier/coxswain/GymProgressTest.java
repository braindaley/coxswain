package svenmeier.coxswain;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import propoid.db.Repository;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GymProgressTest {

    private Context context;
    private Gym gym;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("gym");

        Constructor<Gym> constructor = Gym.class.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        gym = constructor.newInstance(context);
    }

    @After
    public void tearDown() throws Exception {
        if (gym != null) {
            Field repositoryField = Gym.class.getDeclaredField("repository");
            repositoryField.setAccessible(true);
            ((Repository) repositoryField.get(gym)).close();
        }
        context.deleteDatabase("gym");
    }

    @Test
    public void progressUsesValuesRelativeToSegmentStart() {
        Segment segment = new Segment(Difficulty.HARD).setDistance(1000).setStrokeRate(24);
        Measurement start = measurement(20, 100, 20);
        gym.onMeasured(start);
        Gym.Progress progress = gym.new Progress(segment, start);
        gym.progress = progress;

        gym.onMeasured(measurement(80, 350, 23));
        assertEquals(250, progress.achieved());
        assertEquals(0.25f, progress.completion(), 0.001f);
        assertFalse(progress.inLimit());

        gym.onMeasured(measurement(260, 1200, 24));
        assertEquals(1.0f, progress.completion(), 0.001f);
        assertTrue(progress.inLimit());
    }

    @Test
    public void transitionsAcrossDistanceAndDurationSegments() {
        Program program = new Program("Mixed");
        program.getSegments().clear();
        program.addSegment(new Segment(Difficulty.HARD).setDistance(100));
        program.addSegment(new Segment(Difficulty.REST).setDuration(10));
        gym.select(program);

        assertEquals(Event.PROGRAM_START, gym.onMeasured(measurement(1, 10, 24)));
        assertEquals(Event.SEGMENT_CHANGED, gym.onMeasured(measurement(5, 100, 24)));
        assertEquals(Event.PROGRAM_FINISHED, gym.onMeasured(measurement(15, 110, 24)));
        assertEquals(15, gym.current.duration.get().intValue());
        assertEquals(110, gym.current.distance.get().intValue());
    }

    private Measurement measurement(int duration, int distance, int strokeRate) {
        Measurement measurement = new Measurement();
        measurement.setDuration(duration);
        measurement.setDistance(distance);
        measurement.setStrokeRate(strokeRate);
        return measurement;
    }
}
