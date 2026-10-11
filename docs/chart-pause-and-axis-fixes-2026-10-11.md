# Pause-aware split charts and crowded thresholds

The supplied earlier-build screenshots show a split outlier compressing the pace trace and its threshold labels near the top. Power values 65–77 W are genuinely close relative to the row’s output; spreading the actual guides would misrepresent the scale.

Display-only split handling now hides readings when cadence is zero and the monitor also reports zero power or unchanged distance/strokes. After a confirmed idle or clock pause, a split more than twice the nearby moving median is omitted for up to ten seconds within that same interval. Unsupported cadence devices and genuine slow work remain visible. Original measurements, statistics, history and race comparisons are unchanged. Missing plotted readings split the ten-second display bins and curves.

Right-axis labels now maintain readable spacing with short colored connectors to their true guide positions. The plot height grows when necessary for larger text. Thresholds are not automatically changed; new suggestions can be reviewed in Settings after additional completed interval rows. Existing recorded profiles retain their thresholds. Heart-rate filtering and zone calculations are unchanged. Stroke-rate ranges are available in the newer cadence build after configuring and saving them in Settings.

Validation: debug app and instrumentation builds passed; all 30 chart-data unit tests and seven isolated emulator chart checks passed. Regression fixtures cover idle/resume split outliers, actual clock pauses, genuine slow rowing, devices without cadence, and the screenshot’s 65/69/73/77 W label cluster. The supplied phone screenshots were reviewed; the original phone recording was not imported or modified.
