package svenmeier.coxswain.gym;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/** A stable, detached representation of the workout requested at session start. */
public final class WorkoutDefinition {

    private WorkoutDefinition() {
    }

    public static String freeze(Program program) {
        if (program == null) {
            return null;
        }
        try {
            JSONObject root = new JSONObject();
            root.put("name", program.name.get());
            JSONArray segments = new JSONArray();
            for (Segment segment : program.getSegments()) {
                JSONObject value = new JSONObject();
                value.put("name", segment.name.get() == null ? "" : segment.name.get());
                value.put("difficulty", segment.difficulty.get().name());
                value.put("distance", segment.distance.get());
                value.put("duration", segment.duration.get());
                value.put("strokes", segment.strokes.get());
                value.put("energy", segment.energy.get());
                value.put("speed", segment.speed.get());
                value.put("strokeRate", segment.strokeRate.get());
                value.put("pulse", segment.pulse.get());
                value.put("power", segment.power.get());
                segments.put(value);
            }
            root.put("segments", segments);
            return root.toString();
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * Returns a stable compatibility signature for racing. The program name is
     * deliberately excluded: renaming or duplicating an unchanged workout must
     * not invalidate its history.
     */
    public static String compatibilityKey(Program program) {
        if (program == null) return null;
        return compatibilityKey(freeze(program));
    }

    public static String compatibilityKey(String definition) {
        if (definition == null || definition.isEmpty()) return null;
        try {
            JSONArray source = new JSONObject(definition).getJSONArray("segments");
            JSONArray compatible = new JSONArray();
            for (int index = 0; index < source.length(); index++) {
                JSONObject segment = new JSONObject(source.getJSONObject(index).toString());
                segment.remove("name");
                compatible.put(segment);
            }
            return compatible.toString();
        } catch (JSONException invalidDefinition) {
            return null;
        }
    }

    public static Program thaw(String definition) {
        if (definition == null || definition.isEmpty()) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(definition);
            Program program = new Program();
            program.name.set(root.optString("name", "Workout"));
            program.segments.set(new ArrayList<>());
            JSONArray segments = root.getJSONArray("segments");
            for (int index = 0; index < segments.length(); index++) {
                JSONObject value = segments.getJSONObject(index);
                Segment segment = new Segment();
                segment.name.set(value.optString("name", ""));
                segment.difficulty.set(Difficulty.valueOf(value.optString("difficulty", Difficulty.EASY.name())));
                segment.distance.set(value.optInt("distance"));
                segment.duration.set(value.optInt("duration"));
                segment.strokes.set(value.optInt("strokes"));
                segment.energy.set(value.optInt("energy"));
                segment.speed.set(value.optInt("speed"));
                segment.strokeRate.set(value.optInt("strokeRate"));
                segment.pulse.set(value.optInt("pulse"));
                segment.power.set(value.optInt("power"));
                program.addSegment(segment);
            }
            return program;
        } catch (JSONException | IllegalArgumentException invalidDefinition) {
            throw new IllegalArgumentException("Invalid frozen workout definition", invalidDefinition);
        }
    }

    public static SessionType typeOf(Program program) {
        if (program == null) {
            return SessionType.FREE;
        }
        if (program.getSegmentsCount() != 1) {
            return SessionType.INTERVAL;
        }
        Segment segment = program.getSegment(0);
        if (segment.duration.get() > 0) {
            return SessionType.DURATION;
        }
        if (segment.distance.get() > 0) {
            return SessionType.DISTANCE;
        }
        return SessionType.INTERVAL;
    }

    public static PerformanceGoal goalOf(Program program) {
        if (program != null) {
            for (Segment segment : program.getSegments()) {
                if (segment.strokeRate.get() > 0) return PerformanceGoal.STROKE_RATE;
                if (segment.speed.get() > 0) return PerformanceGoal.SPEED;
                if (segment.power.get() > 0) return PerformanceGoal.POWER;
                if (segment.pulse.get() > 0) return PerformanceGoal.PULSE;
            }
        }
        return PerformanceGoal.NONE;
    }

    public static int goalTargetOf(Program program) {
        if (program != null) {
            for (Segment segment : program.getSegments()) {
                int limit = segment.getLimit();
                if (limit > 0) return limit;
            }
        }
        return 0;
    }
}
