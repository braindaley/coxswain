# Bug review fixes — October 7, 2026

Review and fixes include the latest automatic Pete’s Plan time-estimate changes.

## Confirmed phone crash

The connected phone recorded two Coxswain crashes on October 6 at 05:18:39 and 05:19:02. Both reported a null integer in `RaceReplay.java:34`, reached from `Gym.paceReplay()` and the Live Row side rail. Older migrated workouts have nullable rowing-only totals. Replay now uses recorded samples or whole-workout fallback when those fields are absent. Workout statistics also tolerate absent active totals and legacy sample times. No historical rows are deleted or changed by the fix. Emulator testing also exposed a cursor-lifetime error when statistics iterate a database-backed sample list more than once; samples are now materialized into memory before calculation.

## Other fixes

- Bluetooth permissions and system Bluetooth activation are obtained by a visible setup activity before starting the connection service. The FTMS service no longer requests the location foreground-service type. A permission revocation produces feedback instead of an uncaught service failure.
- Selecting a new row finalizes the previous recorded session before resetting its counters. Unfinished structured rows remain in History as ended early; free rows are saved as completed. The replacement does not launch a second completion screen.
- Timed interval transitions interpolate the prescribed boundary and carry elapsed progress into later segments. Delayed packets can cross multiple intervals without extending each prescribed duration. Boundary snapshots preserve work/rest labels.
- Backup/restore includes the six-slot Live Row layout, validates it before database mutations, and remains compatible with older backups.
- Health Connect sync tracks exported permission types. Granting additional metric permissions permits backfill; repeated sync with unchanged permissions is skipped. Legacy boolean sync markers are insufficient to suppress metric backfill. History sync queries all finalized rows rather than the currently selected program.
- German and French translations fix the lint errors introduced by the Settings repair.
- Listener dispatch uses a stable copy so a listener removed during navigation cannot invalidate iteration.

## Verification

Unit regressions cover the exact migrated-null replay crash, statistics for migrated rows, absent sample timestamps, session replacement, delayed interval boundaries, layout restore, and newly granted Health Connect permissions. Emulator launch tests cover Pete’s Plan selection, distance, duration, intervals, and racing against an older workout with missing active totals. Final verification: 107 unit tests pass; both emulator launch tests pass; debug and instrumentation APK builds succeed. Lint reports no errors and nine warnings under the existing baseline. The emulator installation uses a separate application ID and test-only provider namespace, so existing phone/emulator app data is untouched. USB/FTMS hardware transport still requires a real rowing session.
