# History chart update — October 8, 2026

Workout Details and Workout Complete share the updated charts.

## Display and interaction

- The horizontal axis uses local clock time, with recorded sample timestamps preserving pauses. Start and finish are shown with seconds; short workouts and interval breakdowns also use seconds. The inspector shows both clock time and elapsed rowing time.
- Older/imported recordings with absent or invalid sample timestamps use an explicitly estimated timeline between the stored start and finish. Legacy interval boundaries are reconstructed from the frozen program targets and labelled estimated.
- Charts are 256 dp tall, with readable numeric axes. Pace uses minutes:seconds per 500 m and places faster pace higher. Domains cover the observed work values, averages, and targets with padding. Rest is separated rather than allowed to distort work scales.
- The initial stroke window ends at the sixth recorded stroke or 30 elapsed seconds, whichever comes first. Its readings remain available. By default they appear faintly and do not determine the work scale; off-scale readings appear as edge marks. The checkbox restores the full startup scale. This is a labelled display choice, not a sensor-error determination or a deletion of recorded effort.
- Startup zeros before a measure's first positive reading appear as missing values. Later genuine zero readings are retained. No extra smoothing is applied, so short pyramid/interval changes are preserved.
- A shared slider and chart taps select a recorded sample across all three metrics. Selected clock times and values also appear beside each chart. Recorded pause gaps and interval transitions break lines.
- Dotted lines show the workout average; dashed lines show applicable per-step targets. Programmed effort colors and rest shading distinguish steps. These describe prescribed difficulty, not measured physiological intensity zones.
- The interval breakdown includes names, clock ranges, duration, distance, average split, power, stroke rate, applicable target, and average-minus-target. Selecting an interval selects its midpoint on the charts.

## Persistence

Database version 7 adds `Snapshot.intervalIndex` and `Snapshot.intervalStart`, preserving existing rows. Gym records both fields on regular and interpolated boundary samples, including consecutive steps with the same difficulty. Backup exports and restores these optional fields; old backups default to unknown interval identity. Recorded totals, race scoring, and coaching references are unchanged.

## Validation

- 119 unit tests pass, including startup outliers, genuine later zeros, retained pyramid peaks, pause-aware clocks, invalid and legacy clock fallback, identical-effort interval identity, reconstructed distance/timed steps, rest summaries, empty histories, schema migration, and interval identity backup/restore.
- Debug and instrumentation APK builds succeed.
- Lint reports zero new errors and 10 warnings under the existing baseline.
- Both chart interaction tests pass on the emulator: light mode and dark mode with 1.3× text. They exercise startup scaling, chart selection, and the interval breakdown. Screenshots were visually inspected.
- Both Android SQLite data tests pass: migration of the v1 fixture and backup/restore through Android storage. They use a separate review application ID, database, and remapped notification authority. Production app data is not used by the device tests.
