/*
 * Copyright 2015 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package svenmeier.coxswain;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.annotation.UiThread;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import propoid.core.Propoid;
import propoid.db.LookupException;
import propoid.db.Match;
import propoid.db.Order;
import propoid.db.Range;
import propoid.db.Reference;
import propoid.db.Repository;
import propoid.db.Transaction;
import propoid.db.Where;
import propoid.db.aspect.Row;
import propoid.db.cascading.DefaultCascading;
import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.RaceOutcome;
import svenmeier.coxswain.gym.SessionType;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.gym.WorkoutDefinition;
import svenmeier.coxswain.gym.WorkoutStatus;
import svenmeier.coxswain.io.Export;

import static propoid.db.Where.all;
import static propoid.db.Where.equal;
import static propoid.db.Where.greaterEqual;
import static propoid.db.Where.lessThan;

public class Gym {

    private static Gym instance;

    private Context context;

    private Preference<Boolean> external;

    private Repository repository;

    private List<Listener> listeners = new ArrayList<>();

    /**
     * The last measurement.
     */
    private Measurement measurement = new Measurement();

    private Measurement rawMeasurement = new Measurement();

    private Measurement pausedAtMeasurement;

    private Measurement pausedOffsets = new Measurement();

    private boolean paused;

    private long pausedAtMillis;

    private long pausedMillis;

    private SessionType sessionType;

    /** Current hardware connection state, updated by GymService callbacks. */
    public volatile boolean connected;
    public volatile boolean connecting;

    /** Human-readable name reported by the active rowing machine. */
    public volatile String connectedRowerName;
    public volatile String heartSourceName;

    private long sessionGeneration;

    /**
     * The selected program.
     */
    public Program program;

	/**
     * Optional pace workout.
     */
    public Workout pace;

	/**
     * The current workout.
     */
    public Workout current;

	/**
     * Progress of current workout.
     */
    public Progress progress;

    private Gym(final Context context) {

        this.context = context;

        repository = new Repository(context, new GymLocator(context), new GymVersioning());

        external = Preference.getBoolean(context, R.string.preference_data_external);
        external.listen(new Preference.OnChangeListener() {
            @Override
            public void onChanged() {
                repository.close();
                repository.open();

                measurement = new Measurement();
                rawMeasurement = new Measurement();
                current = null;
                progress = null;

                fireChanged(null);
            }
        });
    }

    void initialize() {
        // programs cascade to their segments
        ((DefaultCascading) repository.cascading).setCascaded(new Program().segments);

        // index workout by start
        Workout workoutIndex = new Workout();
        repository.index(workoutIndex, false, Order.descending(workoutIndex.start));

        // index snapshots by workout
        Snapshot snapshotIndex = new Snapshot();
        repository.index(snapshotIndex, false, Order.ascending(snapshotIndex.workout));
        
        Match<Program> query = repository.query(new Program());
        if (query.count() == 0) {
            repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 500), 500, Difficulty.EASY));
            repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 1000), 1000, Difficulty.EASY));
            repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 2000), 2000, Difficulty.MEDIUM));

            repository.insert(Program.kilocalories(String.format(context.getString(R.string.energy_kilocalories), 200), 200, Difficulty.MEDIUM));

            repository.insert(Program.minutes(String.format(context.getString(R.string.duration_minutes), 5), 5, Difficulty.EASY));
            repository.insert(Program.minutes(String.format(context.getString(R.string.duration_minutes), 10), 10, Difficulty.MEDIUM));

            repository.insert(Program.strokes(String.format(context.getString(R.string.strokes_count), 500), 500, Difficulty.MEDIUM));

            Program program = new Program(context.getString(R.string.program_name_segments));
            program.getSegment(0).setDistance(1000);
            program.addSegment(new Segment(Difficulty.HARD).setDuration(60).setStrokeRate(30));
            program.addSegment(new Segment(Difficulty.EASY).setDistance(1000));
            program.addSegment(new Segment(Difficulty.HARD).setDuration(60).setStrokeRate(30));
            program.addSegment(new Segment(Difficulty.EASY).setDistance(1000));
            program.addSegment(new Segment(Difficulty.HARD).setDuration(60).setStrokeRate(30));
            program.addSegment(new Segment(Difficulty.EASY).setDistance(1000));
            program.addSegment(new Segment(Difficulty.HARD).setDuration(60).setStrokeRate(30));
            program.addSegment(new Segment(Difficulty.EASY).setDistance(1000));
            repository.insert(program);
        }
    }

    /**
     * Compact workouts.
     *
     * @param count maximum count of workouts to compact
     */
    public void compact(int count) {
        int days = Preference.getInt(context, R.string.preference_compact).fallback(180).get();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -days);
        Workout workout = new Workout();
        Snapshot snapshot = new Snapshot();
        Where where =
                all(
                    Where.lessEqual(workout.start, calendar.getTimeInMillis()),
                        Where.is(snapshot.workout, Where.any())
                );
        for (Workout compact : repository.query(workout, where).list(Range.limit(count), Order.ascending(workout.start))) {
            repository.query(snapshot, equal(snapshot.workout, compact)).delete();
        }

        repository.vacuum();
    }

    /**
     * Add a listener - has to be called on main thread.
     * 
     * @param listener listener of changes in the gym
     */
    @UiThread
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Match<Program> getPrograms() {
        return repository.query(new Program());
    }

    public Program getProgram(Reference<Program> reference) {
        return repository.lookup(reference);
    }

    public <P extends Propoid> P get(Reference<P> reference) {
        return repository.lookup(reference);
    }

    public void add(final String programName, final Workout workout, final List<Snapshot> snapshots) {

        repository.transactional(new Transaction() {
            @Override
            public void doTransactional() {
                Program example = new Program();

                // imported workouts are not evaluated by default
                workout.evaluate.set(false);

                Program linked = repository.query(example, equal(example.name, programName)).first();
                workout.program.set(linked);
                if (linked != null) {
                    workout.freeze(linked, WorkoutDefinition.typeOf(linked));
                } else {
                    workout.programName.set(programName);
                    workout.sessionType.set(SessionType.FREE);
                }
                workout.status.set(WorkoutStatus.COMPLETED);
                workout.completed.set(workout.start.get() + workout.duration.get() * 1000L);
                repository.merge(workout);

                for (Snapshot snapshot : snapshots) {
                    snapshot.workout.set(workout);

                    repository.merge(snapshot);
                }
            }
        });
    }

    public void mergeProgram(Program program) {
        repository.merge(program);
    }

    public void mergeSegment(Segment segment) {
        repository.merge(segment);
    }

    public Match<Workout> getWorkouts() {
        if (program == null) {
            return getAllWorkouts();
        }
        Workout prototype = new Workout();
        Where finalized = Where.any(
                equal(prototype.status, WorkoutStatus.COMPLETED),
                equal(prototype.status, WorkoutStatus.ENDED_EARLY));

        if (Row.getID(program) == Row.TRANSIENT) {
            return repository.query(prototype, Where.none());
        } else {
            return repository.query(prototype, all(equal(prototype.program, program), finalized));
        }
    }

    /** All completed workouts, independent of the currently selected workout program. */
    public Match<Workout> getAllWorkouts() {
        Workout prototype = new Workout();
        Where finalized = Where.any(
                equal(prototype.status, WorkoutStatus.COMPLETED),
                equal(prototype.status, WorkoutStatus.ENDED_EARLY));
        return repository.query(prototype, finalized);
    }

    /** All evaluated completed workouts in the requested time window, independent of selection. */
    public Match<Workout> getAllWorkouts(long from, long to) {
        Workout prototype = new Workout();
        return repository.query(prototype, all(
                equal(prototype.evaluate, true),
                Where.any(
                        equal(prototype.status, WorkoutStatus.COMPLETED),
                        equal(prototype.status, WorkoutStatus.ENDED_EARLY)),
                greaterEqual(prototype.start, from),
                lessThan(prototype.start, to)));
    }

    public boolean hasWorkoutHistory(Program selectedProgram) {
        if (selectedProgram == null || Row.getID(selectedProgram) == Row.TRANSIENT) return false;
        Workout prototype = new Workout();
        return repository.query(prototype, all(
                equal(prototype.program, selectedProgram),
                Where.any(equal(prototype.status, WorkoutStatus.COMPLETED), equal(prototype.status, WorkoutStatus.ENDED_EARLY))))
                .count() > 0;
    }

    public List<Workout> getRaceCandidates(Program selectedProgram) {
        if (selectedProgram == null) return new ArrayList<>();
        Workout prototype = new Workout();
        boolean interval = WorkoutDefinition.typeOf(selectedProgram) == SessionType.INTERVAL;
        String compatibility = WorkoutDefinition.compatibilityKey(selectedProgram);
        List<Workout> candidates = new ArrayList<>();
        for (Workout candidate : repository.query(prototype, Where.any(
                equal(prototype.status, WorkoutStatus.COMPLETED),
                equal(prototype.status, WorkoutStatus.ENDED_EARLY))).list()) {
            if (interval && candidate.status.get() != WorkoutStatus.COMPLETED) continue;
            if (compatibility.equals(WorkoutDefinition.compatibilityKey(candidate.programDefinition.get()))) {
                candidates.add(candidate);
            }
        }
        Collections.sort(candidates, (left, right) -> {
            boolean timed = WorkoutDefinition.typeOf(selectedProgram) == SessionType.DURATION;
            int leftValue = timed ? left.distance.get() : left.duration.get();
            int rightValue = timed ? right.distance.get() : right.duration.get();
            return timed ? Integer.compare(rightValue, leftValue) : Integer.compare(leftValue, rightValue);
        });
        return candidates;
    }

    public Match<Workout> getWorkouts(long from, long to) {
        Workout prototype = new Workout();

        // evaluated workouts only
        if (program == null) {
            return repository.query(prototype, all(
                    equal(prototype.evaluate, true),
                    Where.any(
                            equal(prototype.status, WorkoutStatus.COMPLETED),
                            equal(prototype.status, WorkoutStatus.ENDED_EARLY)),
                    greaterEqual(prototype.start, from),
                    lessThan(prototype.start, to))
            );
        } else if (Row.getID(program) == Row.TRANSIENT) {
            return repository.query(prototype, Where.none());
        } else  {
            return repository.query(prototype, all(
                    equal(prototype.program, program),
                    equal(prototype.evaluate, true),
                    Where.any(
                            equal(prototype.status, WorkoutStatus.COMPLETED),
                            equal(prototype.status, WorkoutStatus.ENDED_EARLY)),
                    greaterEqual(prototype.start, from),
                    lessThan(prototype.start, to))
            );
        }
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public void delete(Propoid propoid) {
        if (propoid instanceof Workout) {
            // delete all snapshots of workout
            Snapshot prototype = new Snapshot();
            repository.query(prototype, equal(prototype.workout, (Workout) propoid)).delete();
        }

        repository.delete(propoid);

        if (propoid instanceof Program) {
            // keep one program at least
            if (repository.query(propoid).count() == 0) {
                newProgram();
            }
        }
    }

    public void mergeWorkout(Workout workout) {
        repository.merge(workout);
    }

    public void deselect() {
        if (current != null && current.status.get() == WorkoutStatus.ACTIVE) {
            discard();
        } else {
            clearSession();
        }
    }

    public void select(Program program) {
        start(program, WorkoutDefinition.typeOf(program));
    }

    public void start(Program program, SessionType type) {
        this.pace = null;
        this.program = program;
        prepareSession(type);
    }

    public void startFreeRow() {
        this.pace = null;
        this.program = null;
        prepareSession(SessionType.FREE);
    }

    public void repeat(Workout pace) {
        Program program;
        try {
            program = pace.program.get();
        } catch (LookupException programAlreadyDeleted) {
            // fall back to challenge
            challenge(pace);
            return;
        }

        this.pace = pace;
        this.program = program;
        prepareSession(SessionType.RACE);
    }

    public void challenge(Workout pace) {
        this.pace = pace;
        this.program = Program.meters(context.getString(R.string.action_challenge), pace.distance.get(), Difficulty.NONE);
        prepareSession(SessionType.RACE);
    }

    private void prepareSession(SessionType type) {
        this.sessionType = type;
        this.measurement = new Measurement();
        this.rawMeasurement = new Measurement();
        this.pausedOffsets = new Measurement();
        this.pausedAtMeasurement = null;
        this.paused = false;
        this.pausedAtMillis = 0;
        this.pausedMillis = 0;
        this.current = null;
        this.progress = null;
        this.sessionGeneration++;
        fireChanged(type);
    }

    private void clearSession() {
        this.pace = null;
        this.program = null;
        this.sessionType = null;
        this.measurement = new Measurement();
        this.rawMeasurement = new Measurement();
        this.pausedOffsets = new Measurement();
        this.pausedAtMeasurement = null;
        this.paused = false;
        this.current = null;
        this.progress = null;
        this.sessionGeneration++;
        fireChanged(null);
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean hasActiveSession() {
        return sessionType != null;
    }

    public long getSessionGeneration() {
        return sessionGeneration;
    }

    public void pause() {
        if (sessionType == null || paused) return;
        paused = true;
        pausedAtMillis = System.currentTimeMillis();
        pausedAtMeasurement = new Measurement(rawMeasurement);
        fireChanged(null);
    }

    public void resume() {
        if (!paused) return;
        long measuredPauseMillis = Math.max(0,
                rawMeasurement.getDuration() - pausedAtMeasurement.getDuration()) * 1000L;
        addPauseOffsets(pausedAtMeasurement, rawMeasurement);
        pausedMillis += Math.max(measuredPauseMillis,
                Math.max(0, System.currentTimeMillis() - pausedAtMillis));
        paused = false;
        pausedAtMeasurement = null;
        updatePausedDuration();
        if (current != null) mergeWorkout(current);
        fireChanged(null);
    }

    /** Preserve an in-progress workout when its rower connection disappears. */
    public void connectionLost() {
        connected = false;
        connecting = false;
        connectedRowerName = null;
        heartSourceName = null;
        if (hasActiveSession()) {
            pause();
        } else {
            fireChanged(null);
        }
    }

    public Workout complete() {
        return finalizeSession(WorkoutStatus.COMPLETED);
    }

    public Workout endEarly() {
        WorkoutStatus status = sessionType == SessionType.FREE || progress == null
                ? WorkoutStatus.COMPLETED : WorkoutStatus.ENDED_EARLY;
        return finalizeSession(status);
    }

    public Workout discard() {
        return finalizeSession(WorkoutStatus.DISCARDED);
    }

    private Workout finalizeSession(WorkoutStatus status) {
        if (current == null) {
            clearSession();
            return null;
        }
        if (paused) resume();
        Workout finalized = current;
        finalized.status.set(status);
        if (status == WorkoutStatus.ENDED_EARLY) {
            DiagnosticsLog.record(context, "Workout ended before its target");
        }
        finalized.completed.set(System.currentTimeMillis());
        finalizeRace(finalized);
        updatePausedDuration();
        mergeWorkout(finalized);
        if (status != WorkoutStatus.DISCARDED) {
            Export.start(context, finalized);
        }
        clearSession();
        fireChanged(finalized);
        return finalized;
    }

    private void finalizeRace(Workout workout) {
        if (pace == null || workout.sessionType.get() != SessionType.RACE) return;
        workout.raceReference.set(pace);
        int margin;
        if (program != null && program.getSegmentsCount() == 1
                && program.getSegment(0).duration.get() > 0) {
            margin = workout.distance.get() - pace.distance.get();
        } else {
            margin = (pace.duration.get() - workout.duration.get()) * 1000;
        }
        workout.raceMargin.set(margin);
        workout.raceOutcome.set(margin > 0 ? RaceOutcome.WON
                : margin < 0 ? RaceOutcome.LOST : RaceOutcome.TIED);
    }

    private void updatePausedDuration() {
        if (current != null) {
            current.pausedDuration.set((int) (pausedMillis / 1000L));
        }
    }

	/**
     * A new measurement.
     *
     * @param measurement the measurement
     */
    public Event onMeasured(Measurement measurement) {
        Event event = Event.ACKNOWLEDGED;

        this.rawMeasurement = new Measurement(measurement);

        if (paused) {
            fireChanged(null);
            return event;
        }

        this.measurement = normalized(measurement);

        if (sessionType != null) {
            if (this.measurement.anyTargetValue()) {
                // delay workout creation
                event = analyse(this.measurement);
            }
        }

        fireChanged(this.measurement);

        return event;
    }

    private Event analyse(Measurement measurement) {
        Event event = Event.ACKNOWLEDGED;

        if (current == null) {
            current = program == null ? new Workout(null) : program.newWorkout();
            current.freeze(program, sessionType);
            if (pace != null) current.raceReference.set(pace);
            current.location.set(getLocation());
            mergeWorkout(current);

            progress = program == null || program.getSegmentsCount() == 0
                    ? null : new Progress(program.getSegment(0), new Measurement());

            fireChanged(current);

            event = Event.PROGRAM_START;
        }

        int seconds = current.duration.get();
        try {
            current.onMeasured(measurement);
        } catch (IllegalArgumentException ex) {
            Log.d(Coxswain.TAG, "illegal measurement " + ex.getMessage());

            return Event.REJECTED;
        }

        seconds = (current.duration.get() - seconds);
        if (seconds > 0) {
            mergeWorkout(current);

            // limit snapshots so this does not take forever
            for (seconds = Math.min(seconds, 10); seconds > 0; seconds--) {
                Snapshot snapshot = new Snapshot(progress == null ? Difficulty.NONE : progress.segment.difficulty.get(), measurement);
                snapshot.workout.set(current);
                repository.insert(snapshot);
            }
        }

        if (progress != null && progress.completion() == 1.0f) {
            Segment next = program.getNextSegment(progress.segment);
            if (next == null) {
                mergeWorkout(current);

                progress = null;

                event = Event.PROGRAM_FINISHED;
            } else {
                progress = new Progress(next, measurement);

                event = Event.SEGMENT_CHANGED;
            }
        }

        return event;
    }

    private Measurement normalized(Measurement raw) {
        Measurement adjusted = new Measurement(raw);
        adjusted.setDuration(Math.max(0, raw.getDuration() - pausedOffsets.getDuration()));
        adjusted.setDistance(Math.max(0, raw.getDistance() - pausedOffsets.getDistance()));
        adjusted.setStrokes(Math.max(0, raw.getStrokes() - pausedOffsets.getStrokes()));
        adjusted.setEnergy(Math.max(0, raw.getEnergy() - pausedOffsets.getEnergy()));
        return adjusted;
    }

    private void addPauseOffsets(Measurement start, Measurement end) {
        if (start == null) return;
        pausedOffsets.setDuration(pausedOffsets.getDuration() + Math.max(0, end.getDuration() - start.getDuration()));
        pausedOffsets.setDistance(pausedOffsets.getDistance() + Math.max(0, end.getDistance() - start.getDistance()));
        pausedOffsets.setStrokes(pausedOffsets.getStrokes() + Math.max(0, end.getStrokes() - start.getStrokes()));
        pausedOffsets.setEnergy(pausedOffsets.getEnergy() + Math.max(0, end.getEnergy() - start.getEnergy()));
    }

    public Match<Snapshot> getSnapshots(Workout workout) {
        Snapshot prototype = new Snapshot();

        return repository.query(prototype, equal(prototype.workout, workout));
    }

    public Location getLocation() {
        Location bestLocation = null;

        try {
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

			if (manager != null) {
				for (String provider : manager.getProviders(true)) {
					Location location = manager.getLastKnownLocation(provider);
					if (location == null) {
						continue;
					}

					if (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy()) {
						bestLocation = location;
					}
				}
			}
        } catch (SecurityException ignored) {
        }

        return bestLocation;
    }

    public Program newProgram() {
        Program program = new Program(context.getString(R.string.program_name_new));
        mergeProgram(program);
        return program;
    }

    public Program duplicateProgram(final Program original) {

        final Program duplicate = new Program(context.getString(R.string.program_name_new));

        repository.transactional(new Transaction() {
            @Override
            public void doTransactional() {
                duplicate.getSegments().clear();

                for (Segment segment : original.getSegments()) {
                    duplicate.addSegment(segment.duplicate());
                }

                repository.merge(duplicate);
            }
        });
        return duplicate;
    }

    public Program duplicateProgram(final Program original, final String name) {
        final Program duplicate = new Program(name);
        repository.transactional(new Transaction() {
            @Override public void doTransactional() {
                duplicate.getSegments().clear();
                for (Segment segment : original.getSegments()) duplicate.addSegment(segment.duplicate());
                repository.merge(duplicate);
            }
        });
        return duplicate;
    }

    /** Creates a portable backup of programs, finalized workouts, snapshots, and preferences. */
    public String createBackup() {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            JSONArray programs = new JSONArray();
            for (Program value : getPrograms().list()) programs.put(new JSONObject(WorkoutDefinition.freeze(value)));
            root.put("programs", programs);

            JSONArray workouts = new JSONArray();
            Workout prototype = new Workout();
            for (Workout value : repository.query(prototype, Where.any(
                    equal(prototype.status, WorkoutStatus.COMPLETED),
                    equal(prototype.status, WorkoutStatus.ENDED_EARLY))).list()) {
                JSONObject item = new JSONObject();
                item.put("start", value.start.get());
                item.put("duration", value.duration.get());
                item.put("distance", value.distance.get());
                item.put("strokes", value.strokes.get());
                item.put("energy", value.energy.get());
                item.put("evaluate", value.evaluate.get());
                item.put("status", value.status.get().name());
                item.put("sessionType", value.sessionType.get().name());
                item.put("programName", value.programName.get());
                item.put("programDefinition", value.programDefinition.get());
                item.put("pausedDuration", value.pausedDuration.get());
                item.put("completed", value.completed.get());
                item.put("goalType", value.goalType.get().name());
                item.put("goalTarget", value.goalTarget.get());
                item.put("raceOutcome", value.raceOutcome.get().name());
                item.put("raceMargin", value.raceMargin.get());
                try { item.put("raceReferenceStart", value.raceReference.get().start.get()); } catch (Exception ignored) {}
                JSONArray snapshots = new JSONArray();
                for (Snapshot snapshot : getSnapshots(value).list()) {
                    JSONObject sample = new JSONObject();
                    sample.put("difficulty", snapshot.difficulty.get().name());
                    sample.put("distance", snapshot.distance.get());
                    sample.put("strokes", snapshot.strokes.get());
                    sample.put("energy", snapshot.energy.get());
                    sample.put("speed", snapshot.speed.get());
                    sample.put("pulse", snapshot.pulse.get());
                    sample.put("strokeRate", snapshot.strokeRate.get());
                    sample.put("strokeRatio", snapshot.strokeRatio.get());
                    sample.put("power", snapshot.power.get());
                    snapshots.put(sample);
                }
                item.put("snapshots", snapshots);
                workouts.put(item);
            }
            root.put("workouts", workouts);

            JSONObject preferences = new JSONObject();
            for (Map.Entry<String, ?> entry : PreferenceManager.getDefaultSharedPreferences(context).getAll().entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Boolean || value instanceof Number || value instanceof String) preferences.put(entry.getKey(), value);
            }
            root.put("preferences", preferences);
            return root.toString(2);
        } catch (JSONException impossible) {
            throw new IllegalStateException("Could not create backup", impossible);
        }
    }

    /** Merges a portable backup, de-duplicating workouts by their original start time. */
    public void restoreBackup(String backup) {
        try {
            JSONObject root = new JSONObject(backup);
            if (root.optInt("version") != 1) throw new IllegalArgumentException("Unsupported backup version");
            JSONArray programs = root.getJSONArray("programs");
            for (int i = 0; i < programs.length(); i++) {
                Program restored = WorkoutDefinition.thaw(programs.getJSONObject(i).toString());
                boolean exists = false;
                for (Program current : getPrograms().list()) {
                    if (current.name.get().equals(restored.name.get()) && WorkoutDefinition.compatibilityKey(current).equals(WorkoutDefinition.compatibilityKey(restored))) { exists = true; break; }
                }
                if (!exists) mergeProgram(restored);
            }

            Map<Long, Workout> restoredByStart = new HashMap<>();
            JSONArray workouts = root.getJSONArray("workouts");
            for (int i = 0; i < workouts.length(); i++) {
                JSONObject item = workouts.getJSONObject(i);
                long start = item.getLong("start");
                Workout example = new Workout();
                Workout value = repository.query(example, equal(example.start, start)).first();
                if (value == null) {
                    value = new Workout();
                    value.start.set(start);
                    value.duration.set(item.getInt("duration"));
                    value.distance.set(item.getInt("distance"));
                    value.strokes.set(item.getInt("strokes"));
                    value.energy.set(item.getInt("energy"));
                    value.evaluate.set(item.optBoolean("evaluate", true));
                    value.status.set(WorkoutStatus.valueOf(item.getString("status")));
                    value.sessionType.set(SessionType.valueOf(item.getString("sessionType")));
                    value.programName.set(item.optString("programName", null));
                    value.programDefinition.set(item.optString("programDefinition", null));
                    String restoredCompatibility = WorkoutDefinition.compatibilityKey(value.programDefinition.get());
                    if (restoredCompatibility != null) {
                        for (Program candidate : getPrograms().list()) {
                            if (restoredCompatibility.equals(WorkoutDefinition.compatibilityKey(candidate))) {
                                value.program.set(candidate);
                                break;
                            }
                        }
                    }
                    value.pausedDuration.set(item.optInt("pausedDuration"));
                    value.completed.set(item.optLong("completed"));
                    value.goalType.set(svenmeier.coxswain.gym.PerformanceGoal.valueOf(item.optString("goalType", "NONE")));
                    value.goalTarget.set(item.optInt("goalTarget"));
                    value.raceOutcome.set(RaceOutcome.valueOf(item.optString("raceOutcome", "NONE")));
                    value.raceMargin.set(item.optInt("raceMargin"));
                    repository.merge(value);
                    JSONArray samples = item.getJSONArray("snapshots");
                    for (int s = 0; s < samples.length(); s++) {
                        JSONObject data = samples.getJSONObject(s);
                        Snapshot snapshot = new Snapshot();
                        snapshot.workout.set(value);
                        snapshot.difficulty.set(Difficulty.valueOf(data.getString("difficulty")));
                        snapshot.distance.set(data.getInt("distance")); snapshot.strokes.set(data.getInt("strokes")); snapshot.energy.set(data.getInt("energy"));
                        snapshot.speed.set(data.getInt("speed")); snapshot.pulse.set(data.getInt("pulse")); snapshot.strokeRate.set(data.getInt("strokeRate"));
                        snapshot.strokeRatio.set(data.getInt("strokeRatio")); snapshot.power.set(data.getInt("power"));
                        repository.merge(snapshot);
                    }
                }
                restoredByStart.put(start, value);
            }
            for (int i = 0; i < workouts.length(); i++) {
                JSONObject item = workouts.getJSONObject(i);
                if (item.has("raceReferenceStart")) {
                    Workout value = restoredByStart.get(item.getLong("start"));
                    Workout reference = restoredByStart.get(item.getLong("raceReferenceStart"));
                    if (value != null && reference != null) { value.raceReference.set(reference); repository.merge(value); }
                }
            }

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            JSONObject preferences = root.optJSONObject("preferences");
            if (preferences != null) {
                java.util.Iterator<String> keys = preferences.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = preferences.get(key);
                    if (value instanceof Boolean) editor.putBoolean(key, (Boolean)value);
                    else if (value instanceof Integer) editor.putInt(key, (Integer)value);
                    else if (value instanceof Long) editor.putLong(key, (Long)value);
                    else if (value instanceof Number) editor.putFloat(key, ((Number)value).floatValue());
                    else if (value instanceof String) editor.putString(key, (String)value);
                }
            }
            editor.apply();
            fireChanged(null);
        } catch (JSONException | IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Invalid Coxswain backup", invalid);
        }
    }

    public class Progress {

        public final Segment segment;

        /**
         * Measurement of start of segment
         */
        private final Measurement startMeasurement;

        Progress(Segment segment, Measurement measurement) {
            this.segment = segment;

            this.startMeasurement = new Measurement(measurement);
        }

        public Measurement getStartMeasurement() {
            return startMeasurement;
        }

        public float completion() {
            float achieved = achieved();
            float target = segment.getTarget();

            return Math.min(achieved / target, 1.0f);
        }

        public int achieved() {
            return achieved(measurement) - achieved(startMeasurement);
        }

        private int achieved(Measurement measurement) {
            if (segment.distance.get() > 0) {
                return measurement.getDistance();
            } else if (segment.strokes.get() > 0) {
                return measurement.getStrokes();
            } else if (segment.energy.get() > 0) {
                return measurement.getEnergy();
            } else if (segment.duration.get() > 0){
                return measurement.getDuration();
            }
            return 0;
        }

        public boolean inLimit() {
            if (measurement.getSpeed() < progress.segment.speed.get()) {
                return false;
            } else if (measurement.getPulse() < progress.segment.pulse.get()) {
                return false;
            } else if (measurement.getStrokeRate() < progress.segment.strokeRate.get()) {
                return false;
			} else if (measurement.getPower() < progress.segment.power.get()) {
				return false;
            }

            return true;
        }

        public String describeTarget() {
            String target = "";

            if (segment.distance.get() > 0) {
                target = String.format(context.getString(R.string.distance_meters), segment.distance.get());
            } else if (segment.strokes.get() > 0) {
                target = String.format(context.getString(R.string.strokes_count), segment.strokes.get());
            } else if (segment.energy.get() > 0) {
                target = String.format(context.getString(R.string.energy_kilocalories), segment.energy.get());
            } else {
                int seconds = segment.duration.get();
                if (seconds > 0) {
                    if (seconds < 60) {
                        target = String.format(context.getString(R.string.duration_seconds), seconds);
                    } else {
                        target = String.format(context.getString(R.string.duration_minutes), Math.round(seconds / 60f));
                    }
                }
            }
            return target;
        }

        public String describeLimit() {
            String limit = "";

            if (segment.strokeRate.get() > 0) {
                limit = String.format(context.getString(R.string.strokeRate_strokesPerMinute), segment.strokeRate.get());
            } else if (segment.speed.get() > 0) {
                limit = String.format(context.getString(R.string.speed_metersPerSecond), segment.speed.get() / 100f);
            } else if (segment.pulse.get() > 0){
                limit = String.format(context.getString(R.string.pulse_beatsPerMinute), segment.pulse.get());
            } else if (segment.power.get() > 0){
                limit = String.format(context.getString(R.string.power_watts), segment.power.get());
            }

            return limit;
        }

        public String describe() {
            StringBuilder description = new StringBuilder();

            description.append(describeTarget());

            String limit = describeLimit();
            if (limit.isEmpty() == false) {
                description.append(", ");
                description.append(limit);
            }

            return description.toString();
        }
    }

    private void fireChanged(Object scope) {
        for (Listener listener : listeners) {
            listener.changed(scope);
        }
    }

    /**
     * Get the singelton Gym - has to be called on the main thread.
     */
    @UiThread
    public synchronized static Gym instance(Context context) {
        if (instance == null) {
            instance = new Gym(context.getApplicationContext());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    instance.initialize();
                }
            }).start();
        }

        return instance;
    }

    public interface Listener {
        void changed(Object scope);
    }
}
