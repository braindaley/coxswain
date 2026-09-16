# Coxswain database schema v1

Phase 0 records the currently shipped repository schema before workout identity and
session lifecycle fields are added.

- SQLite database name: `gym`
- Fixture SQLite `user_version`: `1`
- Current application version after Phase 1: `2`
- Migration fixture: `app/src/test/resources/migrations/gym-v1.sql`
- Fixture verification: `GymSchemaFixtureTest`

The repository creates tables lazily from Propoid properties. The v1 fixture
materializes all four persisted model tables so future migration tests start from
a stable representation of existing user data.

## Program

| Column | SQLite type | Meaning |
|---|---|---|
| `_id` | INTEGER | Primary key |
| `_type` | TEXT | Propoid subtype |
| `name` | TEXT | User-visible program name |
| `segments` | TEXT | Ordered segment IDs, encoded as `{id}{id}` |

## Segment

`_id`, `_type`, `difficulty`, `distance`, `duration`, `strokes`,
`energy`, `speed`, `strokeRate`, `pulse`, and `power`.

## Workout

`_id`, `_type`, `program`, `location`, `start`, `duration`,
`distance`, `strokes`, `energy`, and `evaluate`.

The v1 Workout row links to a mutable Program and does not contain a frozen
definition, session type, completion state, pause duration, or race result. Phase
1 must add those fields through a v2 migration and retain every v1 row.

## Snapshot

`_id`, `_type`, `workout`, `difficulty`, `distance`, `strokes`,
`energy`, `speed`, `pulse`, `strokeRate`, `strokeRatio`, and `power`.

The fixture includes:

- a single-segment 2,000-meter program;
- a free-form Row/Rest interval program;
- three completed workout records, including one whose Program was deleted; and
- representative active and rest snapshots.

The Phase 1 migration continues to run from this fixture rather than generating
a fresh database, so migration behavior remains testable.
