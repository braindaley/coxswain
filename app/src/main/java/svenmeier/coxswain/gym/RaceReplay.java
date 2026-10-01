package svenmeier.coxswain.gym;

import java.util.ArrayList;
import java.util.List;

/** Recorded cumulative distance, interpolated between samples rather than averaged over the row. */
public final class RaceReplay {
    private final List<Float> times = new ArrayList<>();
    private final List<Float> distances = new ArrayList<>();

    public RaceReplay(Workout workout, List<Snapshot> samples) {
        times.add(0f);
        distances.add(0f);
        int duration = Math.max(0, workout.duration.get());
        boolean timed = !samples.isEmpty() && samples.stream().allMatch(s -> s.duration.get() != null && s.duration.get() > 0);
        for (int i = 0; i < samples.size(); i++) {
            Snapshot sample = samples.get(i);
            float time = timed ? sample.duration.get() : duration * (i + 1f) / samples.size();
            time = Math.min(duration, Math.max(times.get(times.size() - 1), time));
            float distance = Math.max(distances.get(distances.size() - 1), Math.min(workout.distance.get(), sample.distance.get()));
            if (time <= 0f) continue;
            if (time == times.get(times.size() - 1)) distances.set(distances.size() - 1, distance);
            else { times.add(time); distances.add(distance); }
        }
        if (duration > times.get(times.size() - 1)) {
            times.add((float) duration); distances.add((float) workout.distance.get());
        } else if (duration > 0) distances.set(distances.size() - 1, (float) workout.distance.get());
    }

    public float distanceAt(float seconds) {
        if (seconds <= 0) return 0;
        for (int i = 1; i < times.size(); i++) {
            if (seconds <= times.get(i)) {
                float ratio = (seconds - times.get(i - 1)) / (times.get(i) - times.get(i - 1));
                return distances.get(i - 1) + ratio * (distances.get(i) - distances.get(i - 1));
            }
        }
        return distances.get(distances.size() - 1);
    }

    public float timeAtDistance(float meters) {
        if (meters <= 0) return 0;
        for (int i = 1; i < distances.size(); i++) {
            if (meters <= distances.get(i) && distances.get(i) > distances.get(i - 1)) {
                float ratio = (meters - distances.get(i - 1)) / (distances.get(i) - distances.get(i - 1));
                return times.get(i - 1) + ratio * (times.get(i) - times.get(i - 1));
            }
        }
        return times.get(times.size() - 1);
    }
}
