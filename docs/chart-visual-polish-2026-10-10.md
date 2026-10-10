# Workout chart visual polish

The Google Health reference prioritizes a large trace, a quiet card background, short axes, and a small legend. The previous Coxswain presentation repeated explanatory paragraphs, threshold lists, statistics and large zone bars around every graph. Its full-height interval borders, top color strips, square segment joins and dense grid competed with the measured trace.

Workout Complete and Workout Details now share rounded chart cards with a 190 dp plot beneath the title. Avg/Best and Avg/Min/Max numbers are available in the info dialog. Units sit below the title. Numeric axes move to the right; start/end clock labels remain below. Lines use round caps and the presentation-only filtering described below; saved samples are unchanged. Rest/pause shading is faint and interval changes are short bottom ticks. Colored dotted level guides replace the grid and programmed-effort strips; unclassified metrics have no level guides. Stroke rate remains neutral.

The legend uses small colored dots and names. Heart-rate zone time fits underneath each name in four equal columns; percentages are available in the info dialog, replacing the separate large time-in-zone section and repeated HR summary rows. Main-view explanations and numeric boundary lists move into each card’s info dialog. The info dialog identifies legacy recordings using current settings and retains the full source explanation, thresholds and valid-time denominator. The initial-anomaly filter, real interval transitions, unavailable-reading gaps, measured colors, frozen-profile precedence, underlying totals and recorded data are unchanged. Existing interval-result cards retain numerical goals and differences.

## Validation

Debug app and instrumentation APKs built successfully. Android lint has no errors and retains the 10 existing warnings (plus 2 hints). All five WorkoutChartsScreenTest checks passed on the isolated emulator: light/dark charts, larger text, heart-rate recovery, and legacy recordings responding to current thresholds. Light and dark screenshots were visually reviewed. The production phone installation and its data were untouched.

## Vigorous gold follow-up

Light-mode Vigorous now uses a brighter gold (#B07800) rather than the previous brown-gold (#986600), consistently for traces, legends and interval result markers. Its contrast is 3.25:1 against the chart card (#E8EEF5) and 3.53:1 against the screen (#F4F7FB). Dark-mode gold remains #FFCB74. The existing classifier assigns Vigorous between the Vigorous and Peak thresholds and splits lines at every boundary, including the reversed pace direction. Gold trace sections depend on the recorded metric reaching that range; colors are not forced into each recording.

## Compact heart-rate time summary

Heart-rate zone names and m:ss times now form one four-column strip, with a small color dot alongside each time. Percentages move into the info dialog. Names remain visible so the summary does not depend on distinguishing red from green. Output-level legends are unchanged.

## User-selected color palette

Light, Moderate, Vigorous and Peak now use blue, teal, gold and green respectively. The user reports difficulty distinguishing red from green and blue from purple. Graph traces, dotted guides, legends and interval markers share the same palette. Dark mode uses lighter versions; labels remain visible. Thresholds and recorded data are unchanged.

## Trace readability follow-up

The measured line is now 1.5 dp with round caps, versus 2.5 dp previously. A three-reading median filters isolated presentation noise only within a continuous interval; it preserves step edges, zero output, missing readings, sparse samples and pauses. It does not modify samples, summary metrics, coaching targets, race playback or time-in-zone totals. Scale padding increases to 20% (at least four seconds for pace), using the original range so extremes are not cropped. The plot stays tall at 260 dp with tighter 12 dp spacing. Current levels and the repeated Started/Finished sentence move into the info dialog, which also discloses filtering. Start/end clock labels remain below the plot.

Validation for this follow-up: debug and instrumentation builds passed; all 19 WorkoutChartDataTest cases and all five WorkoutChartsScreenTest emulator checks passed. Light and dark screenshots, including larger text, were reviewed. Lint has no errors and retains the existing 10 warnings and two hints.

## Remove duplicate card metrics

Avg/Best and Avg/Min/Max rows are removed between each chart title and plot. The chart info dialog retains the metric summary, which remains available to screen readers through the chart description. Main workout summaries and the compact heart-rate zone-time strip remain in place.

## Four level guides

Configured heart-rate, power and pace charts now always display four color-matched dotted guides. The scale includes the guide values so guides cannot disappear when a recording stays within one level. Moderate, Vigorous and Peak use their actual starting thresholds. Light has no lower threshold; its reference uses resting BPM when available, otherwise one threshold spacing into Light (clamped at zero for increasing measures). Pace reverses this direction correctly. The info dialog explains this distinction. Neutral/unconfigured metrics no longer show an average dotted guide that might be confused with a level.

Four-guide validation: debug and instrumentation builds passed, all 21 WorkoutChartDataTest cases and all five emulator chart UI tests passed. The heart-rate screenshot was inspected and shows all four color-matched guides.

## Reference-proportion refinement

The side-by-side phone screenshots showed that 260 dp plots still produced tall cards and the dash effect made guides look faint. Titles and info icons now sit above each card, matching the reference hierarchy. Plot height is 190 dp; cards use a lighter #EEF3F9 background, 28 dp corners and 20/24 dp padding. Guides are explicit opaque circles at 5 dp spacing, rather than dashed paths. The trace median now uses readings within two seconds on either side, confined to continuous same-interval data. It retains boundary readings, zeros, sensor gaps and sustained effort changes; all statistics, zone totals and stored readings remain unchanged.

Reference refinement validation: production and isolated debug/instrumentation builds passed, all 22 chart-data tests and five emulator UI tests passed. Light/dark screenshots were inspected, including larger text and explicit four-color guide dots.

## Bounded curves and aligned axis values

Continuous reading pairs now use monotone Hermite interpolation with limited slopes and endpoint clamping. This rounds the trace without generating values outside each displayed reading pair. Missing readings, phase boundaries and pause breaks retain their existing separation; no saved data or metrics change. Zone color changes are calculated along the rendered curve at the actual thresholds. Configured charts place right-axis values at their four guide positions, using the same fraction and six-dp plot inset. Unconfigured metrics retain min/mid/max labels.

Curve/axis validation: production and isolated debug/instrumentation builds passed. All 23 chart-data tests, including curve endpoint, monotonicity, plateau and no-overshoot checks, passed; all five emulator chart UI tests passed. Light/dark screenshots with normal/larger text were reviewed and guide values align with their dots.

## Final trace weight

The user requested a slightly thicker line after reviewing the reference again. Curved trace strokes are now 2 dp, increased from 1.5 dp; guide dots and all filtering, interpolation and axis behavior are unchanged.
