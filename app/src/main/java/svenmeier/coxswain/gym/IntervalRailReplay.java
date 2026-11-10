package svenmeier.coxswain.gym;

import java.util.ArrayList;
import java.util.List;

/** Display-only replay on the same interval bands as the live row; never used for race scoring. */
public final class IntervalRailReplay {
    private final Program program;
    private final List<Snapshot> samples;
    private final float[] times;
    private final float[] starts;
    private final float duration;

    public IntervalRailReplay(Program program, Workout workout, List<Snapshot> recordings) {
        this.program = program;
        this.samples = new ArrayList<>(recordings);
        duration = Math.max(0, workout.duration.get());
        times = new float[samples.size() + 1];
        boolean timed = !samples.isEmpty() && samples.stream().allMatch(s -> s.duration.get() > 0);
        for (int i = 0; i < samples.size(); i++) {
            times[i + 1] = Math.max(times[i], Math.min(duration,
                    timed ? samples.get(i).duration.get() : duration * (i + 1f) / samples.size()));
        }
        starts = new float[program.getSegmentsCount()];
        java.util.Arrays.fill(starts, Float.NaN);
        starts[0] = 0;
        // New recordings preserve the actual start of each interval, including delayed packets.
        if (timed) for (Snapshot sample : samples) {
            int index = sample.intervalIndex.get();
            int start = sample.intervalStart.get();
            if (index > 0 && index < starts.length && start >= 0 && start <= sample.duration.get()) {
                if (Float.isNaN(starts[index])) starts[index] = start;
                else starts[index] = Math.min(starts[index], start);
            }
        }
        for (int i = 1; i < starts.length; i++) {
            if (Float.isNaN(starts[i])) {
                Segment previous = program.getSegment(i - 1);
                if (samples.isEmpty()) {
                    float total = 0;
                    for (Segment segment : program.getSegments()) total += Math.max(1, segment.asDuration());
                    starts[i] = starts[i - 1] + duration * Math.max(1, previous.asDuration()) / total;
                } else if (previous.distance.get() == 0 && previous.strokes.get() == 0 && previous.energy.get() == 0) {
                    starts[i] = starts[i - 1] + previous.duration.get();
                } else {
                    float target = counterAt(previous, starts[i - 1]) + previous.getTarget();
                    starts[i] = duration;
                    for (int j = 1; j < times.length; j++) {
                        if (times[j] <= starts[i - 1]) continue;
                        float before = Math.max(starts[i - 1], times[j - 1]);
                        float low = times[j - 1] < starts[i - 1] ? counterAt(previous, before) : counter(previous, j - 1);
                        float high = counter(previous, j);
                        if (high >= target && high > low) {
                            starts[i] = before + (times[j] - before) * (target - low) / (high - low);
                            break;
                        }
                    }
                }
            }
            starts[i] = Math.max(starts[i - 1], Math.min(duration, starts[i]));
        }
    }

    public float progressAt(float seconds) {
        if (seconds <= 0 || duration <= 0) return 0;
        if (seconds >= duration) return 1;
        int index = 0;
        while (index + 1 < starts.length && seconds >= starts[index + 1]) index++;
        Segment segment = program.getSegment(index);
        float fraction;
        if (samples.isEmpty()) {
            float end = index + 1 < starts.length ? starts[index + 1] : duration;
            fraction = end > starts[index] ? (seconds - starts[index]) / (end - starts[index]) : 0;
        } else {
            fraction = segment.getTarget() > 0
                    ? (counterAt(segment, seconds) - counterAt(segment, starts[index])) / segment.getTarget() : 0;
        }
        return position(program, index, fraction);
    }

    private float counter(Segment segment, int index) {
        if (index == 0) return 0;
        Snapshot sample = samples.get(index - 1);
        if (segment.distance.get() > 0) return sample.distance.get();
        if (segment.strokes.get() > 0) return sample.strokes.get();
        if (segment.energy.get() > 0) return sample.energy.get();
        return times[index];
    }

    private float counterAt(Segment segment, float time) {
        if (segment.distance.get() == 0 && segment.strokes.get() == 0 && segment.energy.get() == 0) return time;
        for (int i = 1; i < times.length; i++) {
            if (time <= times[i]) {
                float span = times[i] - times[i - 1];
                float fraction = span > 0 ? Math.max(0, (time - times[i - 1]) / span) : 1;
                return counter(segment, i - 1) + fraction * (counter(segment, i) - counter(segment, i - 1));
            }
        }
        return counter(segment, samples.size());
    }

    /** Both marker positions and painted bands use these same weights. */
    public static float position(Program program, int index, float completion) {
        float total = 0, before = 0, weight = 0;
        for (int i = 0; i < program.getSegmentsCount(); i++) {
            float value = Math.max(1, program.getSegment(i).asDuration());
            total += value;
            if (i < index) before += value;
            if (i == index) weight = value;
        }
        return total > 0 ? Math.max(0, Math.min(1, (before + weight * Math.max(0, Math.min(1, completion))) / total)) : 0;
    }

    /** A virtual pace row follows its own prescribed work targets and recovery durations. */
    public static float paceProgress(Program program, int splitSeconds, float seconds) {
        float remaining = Math.max(0, seconds);
        for (int i = 0; i < program.getSegmentsCount(); i++) {
            Segment segment = program.getSegment(i);
            float span = segment.distance.get() > 0 ? segment.distance.get() * splitSeconds / 500f
                    : segment.duration.get() > 0 ? segment.duration.get() : Math.max(1, segment.asDuration());
            if (remaining < span) return position(program, i, remaining / span);
            remaining -= span;
        }
        return 1;
    }
}
