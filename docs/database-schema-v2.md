# Coxswain database schema v2

Schema v2 makes a Workout a durable historical record. The existing `start`
column remains the started timestamp so older queries and exports stay
compatible.

## Added Workout columns

| Column | SQLite type | Meaning |
|---|---|---|
| `sessionType` | TEXT | Free, Duration, Distance, Interval, or Race |
| `programName` | TEXT | Name frozen when the session begins |
| `programDefinition` | TEXT | Ordered segment definition encoded as JSON |
| `status` | TEXT | Active, Completed, Ended Early, or Discarded |
| `pausedDuration` | INTEGER | Total excluded pause time in seconds |
| `completed` | INTEGER | Finalization timestamp in epoch milliseconds |
| `goalType` | TEXT | Optional stroke-rate, speed, power, or pulse goal |
| `goalTarget` | INTEGER | Goal in the engine's native unit |
| `raceReference` | INTEGER | Optional reference Workout ID |
| `raceOutcome` | TEXT | None, Won, Lost, or Tied |
| `raceMargin` | INTEGER | Signed finishing margin for the session type |

The v1-to-v2 migration freezes every linked Program and ordered Segment into the
Workout. When a referenced Program has already been deleted, it creates a
best-effort single-segment definition from the recorded totals. Existing rows
are marked Completed, with `completed` derived from `start + duration`.

History queries return Completed and Ended Early rows. Active and Discarded rows
remain outside normal history.
