# Workout chart visual polish

The Google Health reference prioritizes a large trace, a quiet card background, short axes, and a small legend. The previous Coxswain presentation repeated explanatory paragraphs, threshold lists, statistics and large zone bars around every graph. Its full-height interval borders, top color strips, square segment joins and dense grid competed with the measured trace.

Workout Complete and Workout Details now share rounded chart cards with compact Avg/Best or Avg/Min/Max numbers above a 280 dp plot. Units sit below the title. Numeric axes move to the right; start/end clock labels remain below. Lines use round caps without smoothing or changing samples. Rest/pause shading is faint and interval changes are short bottom ticks. Colored dotted level guides replace the grid and programmed-effort strips; unclassified metrics retain a quiet average guide. Stroke rate remains neutral.

The legend uses small colored dots and names. Heart-rate zone time fits underneath each name in four equal columns; percentages are available in the info dialog, replacing the separate large time-in-zone section and repeated HR summary rows. Main-view explanations and numeric boundary lists move into each card’s info dialog. A small Current levels label identifies legacy recordings using current settings; the dialog retains the full source explanation, thresholds and valid-time denominator. The initial-anomaly filter, real interval transitions, unavailable-reading gaps, measured colors, frozen-profile precedence, underlying totals and recorded data are unchanged. Existing interval-result cards retain numerical goals and differences.

## Validation

Debug app and instrumentation APKs built successfully. Android lint has no errors and retains the 10 existing warnings (plus 2 hints). All five WorkoutChartsScreenTest checks passed on the isolated emulator: light/dark charts, larger text, heart-rate recovery, and legacy recordings responding to current thresholds. Light and dark screenshots were visually reviewed. The production phone installation and its data were untouched.

## Vigorous gold follow-up

Light-mode Vigorous now uses a brighter gold (#B07800) rather than the previous brown-gold (#986600), consistently for traces, legends and interval result markers. Its contrast is 3.25:1 against the chart card (#E8EEF5) and 3.53:1 against the screen (#F4F7FB). Dark-mode gold remains #FFCB74. The existing classifier assigns Vigorous between the Vigorous and Peak thresholds and splits lines at every boundary, including the reversed pace direction. Gold trace sections depend on the recorded metric reaching that range; colors are not forced into each recording.

## Compact heart-rate time summary

Heart-rate zone names and m:ss times now form one four-column strip, with a small color dot alongside each time. Percentages move into the info dialog. Names remain visible so the summary does not depend on distinguishing red from green. Output-level legends are unchanged.
