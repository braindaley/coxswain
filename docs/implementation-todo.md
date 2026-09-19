# Coxswain implementation audit and remaining work

Audited against `Coxswain_App_Implementation_Plan.md` on September 19, 2026.

Status legend: complete means implemented in source; verify means implemented but not proven by the final build/device gate; incomplete means the planned behavior is absent or still a placeholder.

## Release blockers

- [x] Run `:app:assembleDebug`, `testDebugUnitTest`, and `:app:assembleDebugAndroidTest` against the latest Phase 3–6 commits.
- [ ] Run the Compose instrumentation suite on an emulator or physical device.
- [x] Resolve or intentionally baseline current `lintDebug` errors. The 50 legacy findings are captured in `app/lint-baseline.xml`; new lint findings fail the build.
- [ ] Add end-to-end tests for Free Row, Quick Start, intervals, program creation, Race Your Best, History deletion, Row Again, and More destinations.
- [ ] Verify the v1-to-v2 migration against a copy of real user data.

## Phase 0 — Baseline and build confidence

Implemented: CI debug build, unit-test baseline, schema documentation, v1 migration fixture, engine tests, and basic top-level navigation test.

- [x] Expand Compose navigation tests beyond switching the four bottom tabs, including Quick Start variants and every More destination.
- [ ] Add migration fixtures containing multiple programs, completed workouts, deleted-program history, and snapshots.
- [ ] Confirm a clean checkout passes CI after all current changes.

## Phase 1 — Workout identity and session lifecycle

Implemented: v2 workout identity fields, frozen definitions, migration, Free Row, pause/resume, completion/end/discard, pace conversion, and export-after-finalization.

- [ ] Add lifecycle tests for Back, rotation, process recreation, Bluetooth loss, and USB detach during active and paused sessions.
- [ ] Verify paused time and snapshot accumulation on actual hardware.
- [ ] Guard stale notification launches when no active session exists.
- [ ] Confirm every finalization path clears the active Gym session exactly once.

## Phase 2 — Quick Start, Programs, and Library

Implemented: shared Duration/Distance setup, Duration/Distance/Rest interval segments, optional goals, Start/Save, naming prompt, visible Create Program, View/Start/Duplicate/Delete, read-only used programs, and curated library copies.

- [ ] Add unit tests proving immediate-start and saved-program definitions are identical for Duration, Distance, and mixed intervals.
- [ ] Add UI tests for interval type changes, value edits, add, delete, and invalid zero targets.
- [ ] Test duplicate/delete/list refresh and deletion of the final saved program.
- [ ] Verify read-only programs expose no controls that appear editable.
- [ ] Decide whether segment reordering is required; the decorative handle is removed, but reordering is not implemented.

## Phase 3 — Live Row

Implemented: six persisted metric choices, explicit edit mode, radio selection, live measurement values, engine pause/end, Gym progress, connection state, goal variance styling, interval strip, rest view, and race comparison lines.

- [x] Make the goal cell’s signed variance the primary large value, with the live metric and target in smaller labels as designed.
- [x] Add a neutral near-target state and unit-aware variance formatting for SPM, watts, speed, and split.
- [x] Show the actual target value inside the goal cell.
- [x] Replace the generic `Next: Row` rest label with the real next segment and target.
- [x] Add numeric overall interval sequence progress during rest.
- [x] Calculate the saved-best race line at the current elapsed time/distance.
- [x] Display live lead/behind distance without shrinking the metric grid.
- [x] Expose the connected rower name/type and connect/disconnect state for USB and Bluetooth.
- [x] Remove the unused `onEditMetric` callback/TODO from `WorkoutActivity`.
- [ ] Add deterministic tests for goal variance, interval transitions, rest countdown, and race progress.

## Phase 4 — Complete, Details, History, and Race Your Best

Implemented: shared results component, snapshot-backed statistics, charts, Details deletion, compact History rows, race win icon, compatible-result chooser, race start, result reference/outcome/margin, and race summary.

- [x] Convert split-chart samples from speed to `/500 m` pace before graphing.
- [x] Add average/minimum markers to split charts, average/maximum markers to power charts, and average/minimum/maximum/total labels to stroke-rate charts.
- [x] Plot samples against elapsed time and add numbered vertical ticks.
- [x] Add type-appropriate interval primary summaries.
- [x] Format race margins with correct units in Workout Details.
- [x] Exclude ended-early interval results from Race Your Best candidates.
- [x] Define compatibility from segment structure and goals without making the program display name part of compatibility.
- [ ] Add fixtures proving best selection for distance, duration, and mixed intervals.
- [ ] Verify deletion removes snapshots and preserves the source Program.

## Phase 5 — Home

Implemented: Free Row and Quick Start, scrollable Your Rowing card, period controls, real totals, workout-derived graph bars, Last Workout, Details, and Row Again from frozen definitions.

- [x] Use calendar week, calendar month, and calendar year boundaries instead of rolling 7/31/366-day ranges.
- [x] Add meaningful horizontal labels for days/weeks/months and multiple numbered vertical-axis ticks.
- [x] Calculate a real rowing streak and prior-period comparison.
- [x] Add a useful first-workout empty state with a Quick Start action.
- [ ] Verify totals and graph buckets against History fixtures, including year boundaries.
- [ ] Add a small actionable connection treatment only when disconnected, if retained by the final design.

## Phase 6 — Connection, More, Settings, and integrations

Implemented: More shell, Bluetooth FTMS scanner route, existing Settings route, Data & Export route to Settings, legacy units/audio/hardware/heart/storage preferences, Health Connect single-workout export, and engine support for BLE/ANT+ heart rate.

- [x] Build a focused Connect Rower screen showing scan, current device, connecting/connected/disconnected states, disconnect, and remembered-device behavior for Bluetooth and USB.
- [x] Replace Diagnostics and Help informational dialogs with working destinations.
- [x] Build a dedicated Data & Export destination instead of routing to the general Settings screen.
- [x] Surface real rower and heart-rate connection state in Diagnostics.
- [x] List supported heart-rate sources and explain that watches must broadcast BLE HR or ANT+.
- [ ] Add an in-app Health Connect status and permission flow.
- [x] Expose automatic Health Connect export selection clearly.
- [x] Implement explicit existing-history Health Connect sync.
- [x] Persist exported workout identity and stable Health Connect client record IDs so history sync is idempotent.
- [x] Implement backup and restore for programs, workouts, snapshots, race references, and applicable preferences.
- [x] Add a clean-install-style restore test proving programs, results, snapshots, and preferences survive backup/restore.
- [x] Keep troubleshooting content in Diagnostics and remove it from connection surfaces.

## Phase 7 — Accessibility, device validation, and release

Implemented: initial accessibility descriptions for primary Home actions and Live Row metrics.

- [ ] Test small/large phones, portrait, landscape, large fonts, dark mode, and high contrast.
- [ ] Verify all six Live Row values are readable at rowing distance.
- [ ] Test WaterRower USB, Bluetooth FTMS, BLE HR, and ANT+ where hardware is available.
- [ ] Test Bluetooth loss, USB detach, app backgrounding, rotation, and process death during a workout.
- [ ] Audit every Compose control for labels, touch-target size, focus order, and TalkBack behavior.
- [ ] Move new hardcoded English UI strings into resources and verify German/French fallback behavior.
- [ ] Verify locale-aware number, date, time, and unit formatting.
- [ ] Add diagnostics for failed connections, incomplete sessions, export failures, and migration failures.
- [ ] Run migration, backup/restore, and Health Connect device test scripts.
- [ ] Confirm every designed screen is reachable and every visible primary control performs its stated action.

## Recommended execution order

1. Restore a green build/test/lint baseline.
2. Correct Live Row goal, rest, and race calculations.
3. Correct results charts and Race Your Best compatibility.
4. Finish calendar-based Home analytics.
5. Complete Phase 6 connection, Health Connect, backup/restore, Diagnostics, and Help.
6. Finish device, accessibility, lifecycle, migration, and localization validation.

## Validation note

The debug build, unit suite, instrumentation APK build, and lint-with-baseline pass. The Compose suite was installed on the available Android 17 preview emulator, but Espresso fails before app assertions because AndroidX Test calls the removed `android.hardware.input.InputManager.getInstance()` method. A stable Android API emulator or an AndroidX Test release compatible with API 37 is required to complete the device-run checkbox.
