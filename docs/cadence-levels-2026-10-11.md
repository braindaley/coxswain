# Personal cadence ranges

Stroke-rate charts now support Low, Steady, High and Fast ranges in the same blue, teal, gold and green palette. They describe cadence, not effort or physiological heart-rate zones. Colored traces, four dotted guides, aligned SPM values and a compact legend use the saved cadence thresholds. Without configured thresholds the chart remains neutral.

Settings → Power, pace & cadence levels provides three editable ascending SPM thresholds (1–200). Use history suggests the 25th, 50th and 85th percentiles from five-second median bins in the most recent 20 completed rows, excluding rest, untimed recordings and startup anomalies. Flat or insufficient history does not invent ranges. All fields blank disables cadence. Existing customized thresholds should be reviewed before saving history suggestions.

The existing JSON profile gains an optional cadence section; no database schema change is needed. New sessions freeze the section alongside power and pace. New explicit null means cadence is disabled. Earlier recordings lacking the cadence key may use current cadence settings for display, while their recorded power/pace profiles and original data remain untouched. Malformed stored profiles are never replaced.

## Validation

Production and isolated debug/instrumentation builds passed. All 21 targeted model/lifecycle tests passed, including profile persistence with cadence, completed-row suggestion filtering, JSON compatibility and legacy-display fallback. All eight settings/chart UI tests passed, including editing cadence and displaying its legend and SPM thresholds. Lint reports no errors, with the existing 10 warnings and two hints.
