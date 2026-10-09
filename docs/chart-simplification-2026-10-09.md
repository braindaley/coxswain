# Chart simplification — October 9, 2026

This supersedes the startup controls and inspector described in the October 8 chart update.

## Corrections

- Workout Details and Workout Complete no longer show the startup-scale checkbox, startup help text, inspector, slider, crosshairs, or selected-sample labels. The interval breakdown is a read-only summary.
- The previous renderer still drew startup values faintly and placed outliers at the plot edge. It also connected some hidden startup values to the first settled point. Both the rendered trace and its scale now use the same filtered series; omitted points produce gaps, with no edge marks or connecting lines.
- Six strokes were insufficient for the saved WaterRower pace to settle. The initial work step is now checked for a continuous approximately ten-second pace window with at most 3% spread, after six strokes when stroke counts exist. This is a display heuristic, not a determination that a recorded reading was incorrect. The search is limited to 60 seconds and a quarter of the first work step, preserving short efforts. Recordings without initial pace observations are left alone.
- Filtering applies only to the initial settling period. Later interval accelerations, slow steps, high-power peaks, rest boundaries, and clock-time axes remain intact. Saved measurements, workout totals, averages, race comparisons, and coaching references are unchanged.

## Validation

- Rechecked the saved 5,000 m row: the initial 4:46/500 m reading is not plotted. The settling period ends at second 46, and the slowest retained reading is approximately 2:35/500 m.
- 122 unit tests pass, including removal of the startup trace, a ramp continuing beyond stroke six, retention of subsequent slow intervals and power peaks, short first intervals, and unchanged saved totals.
- Debug and instrumentation APK builds succeed; lint reports no errors outside the existing baseline.
- Emulator UI checks cover both themes, including 1.3× text in dark mode, absence of the removed controls, clock-time charts, and the interval breakdown. Tests use a separate review app so production history is untouched.
