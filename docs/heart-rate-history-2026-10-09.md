# Heart-rate history — October 9, 2026

Workout Details and Workout Complete use the shared results component. When a workout contains positive recorded pulse samples, both screens now show average, minimum, and maximum heart rate and a fourth chart with BPM and clock-time axes. No heart-rate section appears for rows without recorded pulse data, regardless of the current sensor setting. Existing historical recordings work without a database migration.

Heart-rate averages use elapsed-time weighting over available readings, including interval rests. A first reading or a reading after an unavailable sample contributes at most one second so it does not backfill an unobserved sensor gap. Duplicate timestamps retain the last sample, as with the other workout statistics. Zero, negative, and null heart-rate readings are missing observations, not zero BPM.

Unlike rowing pace, valid initial heart-rate observations remain visible. The heart-rate chart includes recovery periods and connects across interval transitions, while missing readings and recorded pause gaps break the trace. Programmed-effort colors, rest shading, the average reference, and applicable pulse targets use the existing chart style. Per-interval summaries also show average heart rate when readings were available during that interval. Colors continue to describe programmed effort rather than physiological heart-rate zones.

Labels can wrap without crowding values at larger font sizes. English, German, and French labels are included. Sensor collection, stored measurements, race scoring, and the history list columns are unchanged.

Validation: 127 unit tests pass; debug and instrumentation APK builds succeed; lint has no errors outside the existing baseline. Four isolated emulator UI tests pass, covering presence and absence of heart-rate data in light and dark mode, including 1.3× text. The heart-rate graph screenshot was inspected to confirm recovery plotting and missing-reading gaps.
