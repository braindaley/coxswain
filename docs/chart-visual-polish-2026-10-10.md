# Workout chart visual polish

The Google Health reference prioritizes a large trace, a quiet card background, short axes, and a small legend. The previous Coxswain presentation repeated explanatory paragraphs, threshold lists, statistics and large zone bars around every graph. Its full-height interval borders, top color strips, square segment joins and dense grid competed with the measured trace.

Workout Complete and Workout Details now share rounded chart cards with compact Avg/Best or Avg/Min/Max numbers above a 280 dp plot. Units sit below the title. Numeric axes move to the right; start/end clock labels remain below. Lines use round caps without smoothing or changing samples. Rest/pause shading is faint and interval changes are short bottom ticks. Colored dotted level guides replace the grid and programmed-effort strips; unclassified metrics retain a quiet average guide. Stroke rate remains neutral.

The legend uses small colored dots and names. Heart-rate zone time and percentage fit underneath each name, replacing the separate large time-in-zone section and repeated HR summary rows. Main-view explanations and numeric boundary lists move into each card’s info dialog. A small Current levels label identifies legacy recordings using current settings; the dialog retains the full source explanation, thresholds and valid-time denominator. The initial-anomaly filter, real interval transitions, unavailable-reading gaps, measured colors, frozen-profile precedence, underlying totals and recorded data are unchanged. Existing interval-result cards retain numerical goals and differences.

## Validation

Debug app and instrumentation APKs built successfully. Android lint has no errors and retains the 10 existing warnings (plus 2 hints). All five WorkoutChartsScreenTest checks passed on the isolated emulator: light/dark charts, larger text, heart-rate recovery, and legacy recordings responding to current thresholds. Light and dark screenshots were visually reviewed. The production phone installation and its data were untouched.
