package svenmeier.coxswain;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import propoid.db.SQL;
import propoid.db.schema.Column;
import propoid.db.version.DefaultVersioning;
import propoid.db.version.Upgrade;

/**
 */
class GymVersioning extends DefaultVersioning {

	static final int DATABASE_VERSION = 3;

	GymVersioning() {
		add(new WrongIndices());
		add(new WorkoutIdentity());
		add(new SegmentNames());
	}

	private static class SegmentNames implements Upgrade {
		@Override
		public void apply(SQLiteDatabase database) {
			if (!Column.exists("Segment", database)) return;
			for (Column column : Column.get("Segment", database)) {
				if (column.name.equals("name")) return;
			}
			database.execSQL("ALTER TABLE Segment ADD COLUMN name TEXT");
		}
	}

	private static class WorkoutIdentity implements Upgrade {
		private static final Pattern SEGMENT_ID = Pattern.compile("\\{(\\d+)\\}");

		@Override
		public void apply(SQLiteDatabase database) {
			if (!Column.exists("Workout", database)) {
				return;
			}

			addColumn(database, "sessionType", "TEXT");
			addColumn(database, "programName", "TEXT");
			addColumn(database, "programDefinition", "TEXT");
			addColumn(database, "status", "TEXT");
			addColumn(database, "pausedDuration", "INTEGER");
			addColumn(database, "completed", "INTEGER");
			addColumn(database, "goalType", "TEXT");
			addColumn(database, "goalTarget", "INTEGER");
			addColumn(database, "raceReference", "INTEGER");
			addColumn(database, "raceOutcome", "TEXT");
			addColumn(database, "raceMargin", "INTEGER");

			Cursor workouts = database.rawQuery(
					"SELECT _id, program, start, duration, distance FROM Workout", null);
			try {
				while (workouts.moveToNext()) {
					long id = workouts.getLong(0);
					long programId = workouts.isNull(1) ? -1 : workouts.getLong(1);
					long started = workouts.isNull(2) ? 0 : workouts.getLong(2);
					int duration = workouts.isNull(3) ? 0 : workouts.getInt(3);
					int distance = workouts.isNull(4) ? 0 : workouts.getInt(4);

					Frozen frozen = freeze(database, programId, distance, duration);
					ContentValues values = new ContentValues();
					values.put("sessionType", frozen.type);
					values.put("programName", frozen.name);
					values.put("programDefinition", frozen.definition);
					values.put("status", "COMPLETED");
					values.put("pausedDuration", 0);
					values.put("completed", started + duration * 1000L);
					values.put("goalType", frozen.goalType);
					values.put("goalTarget", frozen.goalTarget);
					values.put("raceOutcome", "NONE");
					values.put("raceMargin", 0);
					database.update("Workout", values, "_id = ?", new String[]{Long.toString(id)});
				}
			} finally {
				workouts.close();
			}
		}

		private void addColumn(SQLiteDatabase database, String name, String type) {
			for (Column column : Column.get("Workout", database)) {
				if (column.name.equals(name)) return;
			}
			database.execSQL("ALTER TABLE Workout ADD COLUMN " + name + " " + type);
		}

		private Frozen freeze(SQLiteDatabase database, long programId, int distance, int duration) {
			String name = "Workout";
			String segmentIds = null;
			if (programId >= 0) {
				Cursor program = database.rawQuery(
						"SELECT name, segments FROM Program WHERE _id = ?",
						new String[]{Long.toString(programId)});
				try {
					if (program.moveToFirst()) {
						name = program.isNull(0) ? name : program.getString(0);
						segmentIds = program.getString(1);
					}
				} finally {
					program.close();
				}
			}

			try {
				JSONArray segments = new JSONArray();
				if (segmentIds != null) {
					Matcher matcher = SEGMENT_ID.matcher(segmentIds);
					while (matcher.find()) {
						JSONObject segment = readSegment(database, Long.parseLong(matcher.group(1)));
						if (segment != null) segments.put(segment);
					}
				}
				if (segments.length() == 0) {
					JSONObject fallback = emptySegment();
					if (distance > 0) fallback.put("distance", distance);
					else fallback.put("duration", Math.max(duration, 1));
					segments.put(fallback);
				}

				JSONObject definition = new JSONObject();
				definition.put("name", name);
				definition.put("segments", segments);
				String type = classify(segments);
				String goalType = "NONE";
				int goalTarget = 0;
				for (int index = 0; index < segments.length() && goalTarget == 0; index++) {
					JSONObject segment = segments.getJSONObject(index);
					if (segment.optInt("strokeRate") > 0) { goalType = "STROKE_RATE"; goalTarget = segment.optInt("strokeRate"); }
					else if (segment.optInt("speed") > 0) { goalType = "SPEED"; goalTarget = segment.optInt("speed"); }
					else if (segment.optInt("power") > 0) { goalType = "POWER"; goalTarget = segment.optInt("power"); }
					else if (segment.optInt("pulse") > 0) { goalType = "PULSE"; goalTarget = segment.optInt("pulse"); }
				}
				return new Frozen(name, definition.toString(), type, goalType, goalTarget);
			} catch (JSONException impossible) {
				throw new IllegalStateException(impossible);
			}
		}

		private JSONObject readSegment(SQLiteDatabase database, long id) throws JSONException {
			Cursor cursor = database.rawQuery(
					"SELECT difficulty, distance, duration, strokes, energy, speed, strokeRate, pulse, power FROM Segment WHERE _id = ?",
					new String[]{Long.toString(id)});
			try {
				if (!cursor.moveToFirst()) return null;
				JSONObject segment = emptySegment();
				segment.put("difficulty", cursor.isNull(0) ? "EASY" : cursor.getString(0));
				String[] names = {"distance", "duration", "strokes", "energy", "speed", "strokeRate", "pulse", "power"};
				for (int index = 0; index < names.length; index++) {
					segment.put(names[index], cursor.isNull(index + 1) ? 0 : cursor.getInt(index + 1));
				}
				return segment;
			} finally {
				cursor.close();
			}
		}

		private JSONObject emptySegment() throws JSONException {
			JSONObject segment = new JSONObject();
			segment.put("difficulty", "EASY");
			for (String name : new String[]{"distance", "duration", "strokes", "energy", "speed", "strokeRate", "pulse", "power"}) {
				segment.put(name, 0);
			}
			return segment;
		}

		private String classify(JSONArray segments) throws JSONException {
			if (segments.length() != 1) return "INTERVAL";
			JSONObject segment = segments.getJSONObject(0);
			if (segment.optInt("duration") > 0) return "DURATION";
			if (segment.optInt("distance") > 0) return "DISTANCE";
			return "INTERVAL";
		}

		private static class Frozen {
			final String name, definition, type, goalType;
			final int goalTarget;

			Frozen(String name, String definition, String type, String goalType, int goalTarget) {
				this.name = name;
				this.definition = definition;
				this.type = type;
				this.goalType = goalType;
				this.goalTarget = goalTarget;
			}
		}
	}

	/**
	 * Index names where bogus, thus they were recreated on each start :/.
	 * Let's drop them all.
	 */
	private class WrongIndices implements Upgrade {
		@Override
		public void apply(SQLiteDatabase database) {
			Cursor indices = database.rawQuery("SELECT name FROM sqlite_master WHERE type = 'index'", new String[0]);
			try {
				while (indices.moveToNext()) {
					String name = indices.getString(0);

					SQL drop = new SQL();
					drop.raw("DROP INDEX ");
					drop.escaped(name);
					database.execSQL(drop.toString());
				}
			} finally {
				indices.close();
			}
		}
	}
}
