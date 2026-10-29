# Split target feedback

Speed-based goals previously selected SPEED (m/s), and the automatic insertion could replace the stroke-rate slot. Values such as +3.3 were m/s differences, not stroke-rate differences. Saved custom layouts could omit the required metric, and split target precision could change when converting through the integer speed representation.

Speed goals now select SPLIT (/500 m). The active goal is guaranteed a display slot without rewriting saved layout preferences; a legacy SPEED slot is preferred for replacement when split is absent. Pete's Plan's goal kind determines the coached metric. Stroke rate receives goal colors only when it is actually the selected goal.

The split tile retains the actual split as its large number. A compact label states seconds faster, on target, or seconds slower and the prescribed target. Text identifies direction independently of color. Pete's Plan uses its exact prescribed seconds rather than deriving the target from rounded speed. Current split and goal feedback use a five-second display average; rate and power feedback use the same window when those are the active goal. Repeated callbacks at the same elapsed second do not add weight. Stopped readings remain unavailable/zero immediately, and interval/session changes reset the window. Raw recordings, history statistics, coaching baselines, and race scoring are unaffected.

Regression coverage includes customized layouts with no split tile, metric selection, exact 3:35 targets, faster/slower semantics, smoothing/reset behavior, full-screen tile coloring and labels, and existing live coaching and heart-rate checks.
