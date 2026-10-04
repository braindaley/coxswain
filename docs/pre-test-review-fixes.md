# Pre-test review fixes

- Reconnecting a rower rebases reset transport counters onto the existing workout, including free rows and paused sessions.
- Ending a race early retains its reference but records no win, loss, tie, or margin.
- Pete's Plan distance benchmarks accept completed rows that overshoot the prescribed distance. Selection and split targets use replay time at the prescribed finish boundary.
- A finished plan retains an overview link so its progress and stop/reset controls remain accessible.
- Workout results calculate average split from rowing time and distance, stroke rate from rowing strokes and time, and power with elapsed-time weighting. Recorded rest is excluded from these averages; zero-power rowing still counts.
- Graphs position recorded samples by timestamp, collapse duplicate timestamps, and shade rest with interval boundary lines. Legacy recordings without timestamps retain estimated spacing; missing recordings cannot recover historical detail.
- Pete's Plan text uses theme primary and secondary text colors for dark-mode contrast.

Verification: debug APK build, complete local unit test suite, Android test APK compilation, and lint. Lint retains the existing eight warnings and baseline-suppressed issues. Physical USB reconnects and on-device appearance remain hardware checks.
