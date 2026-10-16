# Separate recorded peak from training maximum

The phone audit found a saved maximum of 135 BPM, while recorded history contained an instantaneous peak of 152 and a five-second sustained peak of 148. Recorded peaks are evidence of observed HR, not a confirmed physiological maximum. This change does not replace 135 with either observed value.

Heart-rate settings now show the sustained recorded peak as reference data only. History never fills the maximum field and no button copies it into that field. The user enters a known maximum or explicitly selects the age-based estimate; custom zone boundaries remain available. The age-estimate switch and its label share one accessible toggle action.

Profiles include a maximumConfirmed marker for an explicitly chosen maximum. Existing profiles without source metadata must be reviewed rather than silently inherited for new workouts. An existing age-based estimate remains usable. Settings show the previous unverified value, leave the maximum input blank and require user entry or an age estimate before saving. Live tiles do not classify HR using unverified maximum profiles. Existing recorded profiles and samples remain available and unchanged in history and backups. Explicit custom boundaries do not require an inferred maximum.

Only the app code is changed; no phone settings or workout data are edited. Rebuilding requires reviewing Heart-rate zones before relying on new live zone labels when the prior maximum had no confirmed source.

Validation: production debug build, all 187 unit tests and 13 isolated emulator screen checks passed. Regression coverage includes legacy 135 BPM settings, previous-release version-two settings without source metadata, explicit maximum entry, history changes not overwriting inputs, age-estimate selection, new live profiles, and unchanged frozen history.
