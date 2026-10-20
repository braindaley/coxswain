# Settings and interrupted-row recovery cleanup

The output-level settings screen has been removed because result charts derive pace, power, and cadence bands from each row. Heart-rate zone settings remain available. Legacy output profile serialization remains supported for saved history and portable backups.

An ACTIVE row recovered after process restart is still marked ENDED_EARLY. Its finish timestamp is now the later of start + active duration + persisted pause duration, or its latest valid sample timestamp. Sample clocks before the start or after the current time are excluded. Restart time itself is never used as the finish time. This applies to rows recovered after the update; finalized historical rows are not rewritten.

Regression coverage checks that the retired setting is absent, heart-rate settings are available, saved pauses are included, missing/invalid sample clocks use the fallback, a later valid sample is honored, and downtime is excluded. The retired output-settings UI tests are removed with their screen; legacy profile model tests remain.
