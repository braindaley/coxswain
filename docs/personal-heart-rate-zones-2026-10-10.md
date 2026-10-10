# Personal heart-rate zones — October 10, 2026

Settings → Hardware → Heart-rate zones offers calculated or custom zones. Calculated zones use resting heart rate plus 40%, 60%, and 85% of heart-rate reserve (maximum minus resting). Integer lower boundaries round up; a BPM value exactly on a boundary belongs to the higher zone. Users can supply their maximum or estimate it as 220 minus age. An estimate is explicitly labeled. Custom mode accepts three increasing BPM lower boundaries. Settings save as one validated profile, and can be disabled for future workouts.

A recording freezes the profile at its first measurement. Database version 8 adds the nullable `Workout.heartRateZones` field. Both JSON backup and restore preserve it, along with the current profile preference; invalid profiles are rejected before history is replaced. Version-seven and earlier rows keep null thresholds, so later settings do not retroactively classify old history.

Workout Complete and Workout Details color the HR trace blue (Light), green (Moderate), gold (Vigorous), and red (Peak). Colors describe actual measured heart rate, including recovery; interval bands still describe programmed effort. Text labels and BPM boundaries accompany the colored time-in-zone bars. Theme-specific colors support light and dark backgrounds. Other charts retain their existing program colors.

Zone time uses elapsed recording time, interpolating between valid adjacent HR samples and dividing transitions exactly at each BPM boundary. Missing/zero/negative readings and pauses do not contribute zone time. The first reading or a reconnect contributes at most one second. Gaps longer than five seconds are not interpolated or joined in the graph. Duplicate timestamps retain the last sample. Percentages use observed HR time, not the entire workout duration; displayed whole percentages may differ from 100% due to rounding. Raw sensor data is unchanged.

No HR section appears when a workout has no positive HR measurements. Rows without saved thresholds show a plain HR trace and an explanation that personal zones were not recorded. Configure zones before starting a new row to see classification. This version uses locally entered settings; it does not add Health Connect read permissions or Google Health account integration.

Research basis: Google Health documents personalized reserve-based boundaries at https://support.google.com/googlehealth/answer/14237938?hl=en. These app estimates describe recorded exercise data; they are not a prescribed training target.

Validation: 136 unit tests pass, including exact reserve boundaries, zone crossings, missing/sparse readings, recovery, frozen settings, version-seven migration and backup validation. Debug and instrumentation APK builds pass. Eight emulator checks pass across settings, light/dark charts and Android SQLite migration/backup; the dark HR chart screenshot was visually reviewed. Lint has no new errors outside the existing baseline.

## History suggestions

The zone settings automatically prefill empty resting/maximum fields from completed rows with timestamped heart-rate samples. The starting-low suggestion is the lowest sustained reading in the first 30 recorded seconds; the maximum suggestion is the highest sustained reading across completed rows. A qualifying window spans at least five seconds with three or more observations. Its highest BPM supplies the conservative low and its lowest BPM supplies the conservative peak, so a single erroneous low/high reading cannot supply a default. Missing readings, sensor gaps above five seconds, and recorded pause gaps reset the window. Untimed legacy recordings, active sessions and ended-early sessions cannot supply suggestions.

Suggestions load on a background thread and never overwrite an existing saved profile or a field the user has edited. The user can apply updated history suggestions explicitly and then save. The UI identifies the starting low as a resting estimate and the peak as observed rather than a tested maximum. Previously recorded zones remain frozen.

History-suggestion validation: 140 unit tests pass; three isolated emulator settings checks pass, including automatic fill and preservation of manually entered values when history changes. Debug and instrumentation APK builds and lint pass with the existing baseline.
