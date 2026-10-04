# Application review — October 4, 2026

Reviewed local branch `codex/phase-7-validation` at `fa12b51`. This is a source review with build, unit-test and lint verification; it is not a physical rower or phone acceptance test. No application code was changed during this review.

## Resolution after implementation

All twelve findings below have been addressed in the subsequent implementation:

| Finding | Result |
| --- | --- |
| 1 — interrupted workouts | Startup converts persisted ACTIVE rows to ENDED_EARLY so they remain visible and backed up. They do not receive race or plan completion credit. Recovery preserves recorded effort; it does not resume a lost transport session. |
| 2 — rest distance | Every structured row saves rowing-only totals. Race ranking, results, replay and live comparisons exclude recorded rest meters. Older interval rows without recordings or active totals are excluded when those meters cannot be determined. |
| 3 — finish boundary | Measurements stop changing a finished program's result. Fixed-distance scores use interpolated time at the prescribed distance; fixed-time scores use meters at the prescribed time. |
| 4 — restore atomicity | Backup structure and values are validated before mutations. Database restore runs in a transaction; preferences are applied after success. |
| 5 — imports | Initial/new SEND and VIEW intents reach the importer, reject missing URIs, and refresh the visible data after completion. Recreation does not repeat the import. |
| 6 — draft preservation | Program drafts are serialized into activity state. Quick Start type, targets, goals, interval list and pending save state use saved Compose state. |
| 7 — export timeline | New samples preserve wall-clock time, including pause gaps. Health Connect uses those times, recorded elapsed times for older samples, or estimated spacing for untimed legacy samples, and consolidates duplicates. |
| 8 — recording retention | Compaction preserves structured program recordings. Already missing or untimed historical recordings are identified as estimated replay rather than presented as exact. Deleted recordings cannot be reconstructed. |
| 9 — portable preferences | Backups include each program's race toggle and Health Connect sync markers. External-storage selection remains installation-specific. Older backups remain supported. |
| 10 — backup UI | Backup reads/writes and restore work run off the UI thread, with a blocking progress dialog. Null output streams fail explicitly. Restore is blocked during an active session. |
| 11 — program history | History uses program identity or compatible definitions; matching display names alone no longer qualify. |
| 12 — independent toggles | Saved programs have stable UUID identities, including separate copies. Race preferences use that identity, and Home's frozen definition resolves to it. Legacy preferences remain a fallback. |

Database version 6 adds portable program identity and wall-clock sample timing. The legacy schema migration test verifies the new schema. New regressions cover interrupted-row recovery, finish boundaries and overshoot, rest-free race scoring, independent portable toggles, failed restore/retry, benchmark retention, Health Connect timestamps and sync markers. Android recreation tests are included and compiled; execution on a device remains required.

Implementation verification: all 95 unit tests pass; debug APK and Android test APK build successfully. Lint retains the same eight warnings and the existing baseline. Pete's Plan pace estimates also use the shared rowing-only totals from ordinary interval sessions.

## High priority — address before broad testing

1. **Interrupted workouts become inaccessible after process death.** `Gym.initialize()` (Gym.java:165) does not recover or finalize persisted ACTIVE workouts. `WorkoutActivity.onCreate()` (WorkoutActivity.kt:29) closes when the in-memory session is gone, while history and backup queries exclude ACTIVE rows. A killed process leaves recorded effort inaccessible. Recover a paused session from a checkpoint, or convert the saved row to ENDED_EARLY and offer recovery in History. Test process death, relaunch and subsequent backup.

2. **Timed interval races count rest distance as competitive distance.** `getRaceCandidates()` (Gym.java:339) ranks by total distance and `finalizeRace()` (Gym.java:696) compares total distance. Meters accumulated during rest therefore improve the best score. Ordinary intervals do not persist rowing-only totals; Pete rows do. Persist active totals for every program and use them consistently for ranking, live comparisons and outcomes. Test identical work meters with different rest meters.

3. **Finish-boundary scoring is inconsistent with replay.** Distance races rank and score full saved duration (Gym.java:360,699), although Pete distance targets now use replay time at the requested distance. Crossing 5,000m between measurements makes the result depend on the overshoot sample. With automatic end disabled (GymService.java:203), extra time/meters after completion are also included. Capture finish-boundary values once and use them for ranking and outcomes; keep any later activity separate. Test overshoot and continuing after target completion.

4. **Backup restore is neither fully validated nor atomic.** `restoreBackup()` (Gym.java:998) inserts programs and workouts before validating later snapshots and preferences. A malformed later record leaves partial changes despite a failure message. Retrying skips a workout already inserted by start time, so missing snapshots can remain missing. Validate the complete backup first, then apply database mutations in a transaction and preferences only after success. Test failure halfway through and retry.

## Medium priority — functional and reliability improvements

5. **Advertised file imports no longer reach the importer.** AndroidManifest.xml routes SEND/VIEW TCX and Coxswain files to MainActivity, but MainActivity.kt:61 handles only USB attachment. `ImportIntention.onIntent()` still exists and is not called there. Restore routing for both initial and new intents, with URI permission/error handling. Test opening and sharing both supported formats.

6. **Program drafts are lost on activity recreation.** ProgramActivity.kt:103 always rebuilds the draft from the saved program or a new default; the supplied savedInstanceState is not used. Quick Start setup also uses transient Compose state. Rotation or OS recreation loses names, interval changes and targets. Preserve detached drafts with saved state/ViewModel and restore them before rendering. Include a recreation test with multiple named row/rest intervals.

7. **Health Connect exports still use sample indexes as seconds.** Workout2HealthConnect.java:85 places each snapshot at `start + i seconds`, ignoring recorded `Snapshot.duration`. Delayed batches and duplicate snapshots shift or compress the exported timeline. Pauses are also removed from exported elapsed time without retaining wall-clock sample times. Reuse a canonical timestamp-normalization layer and retain enough pause timing for accurate export. Test irregular samples, duplicates and pauses.

8. **Compaction silently changes a recorded race into an average-pace race.** Gym.java:208 deletes snapshots after the configured retention period (default 180 days), including best/reference rows. RaceReplay.java:29 then falls back to a straight line when samples are absent. Preserve recordings used as benchmarks, or clearly identify estimated replay and let users choose another reference. Test racing an old benchmark after compaction.

9. **Backups omit important preferences outside the default preference file.** Gym.java:989 exports only default preferences. Race toggles are in `program_race` (Gym.java:420), and Health Connect export markers are in `health_connect_exports`. Restoring onto another installation changes race behavior and sync status. Define and version the intended portable preference set; exclude device-specific values deliberately rather than accidentally.

10. **Backup work runs synchronously on the UI thread and can falsely report success.** MainActivity.kt:106–111 reads/writes complete backups and performs database operations in activity-result callbacks. A long rowing history can freeze the UI. A null output stream is also treated as successful by the safe-call chain. Move I/O/database work off the main thread, require a usable stream, and show progress plus a clear failure state.

11. **Program history can include unrelated programs with the same name.** ProgramActivity.kt:71–74 accepts either compatible definitions or matching names. Two different programs named “Training” can share the visible history list. Best/average currently use compatible candidates, so those statistics are protected, but the history list is misleading. Use stable program identity and a deliberate compatibility rule, not name equality.

12. **A race preference is shared by all compatible programs.** Gym.java:420–427 keys the toggle by workout definition rather than program identity. Changing one copy's toggle changes another identical program's setting, despite the UI presenting it as a per-program preference. Store per-program preferences; resolve Home “Row again” back to that identity with an explicit fallback.

## Verification and follow-up

Debug build and the 83-test unit suite pass. Lint reports eight warnings; the existing baseline suppresses 61 errors and 265 warnings, so a passing lint task does not establish that those older findings are resolved. Review the baseline separately by severity rather than deleting it wholesale.

Device acceptance still needs actual USB disconnect/reconnect, background/process recreation, Bluetooth permissions, Health Connect permissions, text scaling, small screens, dark mode, and a complete mixed interval race. Existing Android UI tests have previously compiled; this review did not execute them on a device.

Recommended order: session recovery; canonical work/finish-boundary scoring; transactional restore; import routing and draft preservation; timestamp exports and replay retention; preference portability and UI-thread I/O; history identity and per-program toggles.
