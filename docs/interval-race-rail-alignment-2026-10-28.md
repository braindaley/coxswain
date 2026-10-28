# Interval race rail alignment

The interval bands were painted using each segment's estimated duration, including recovery. Race markers were positioned using total rowing distance, excluding recovery. Those different scales made both markers miss interval boundaries and stall through rest.

The live marker now uses the current segment and its actual completion on the same weighted bands used for ordinary interval rows. The saved-row marker uses recorded interval starts and interpolated counters within that interval. Distance, time, strokes, and energy targets are supported. Each row advances through its own recovery band. Virtual Pete's Plan pace targets include their prescribed recoveries on the same rail.

Recordings without interval metadata reconstruct transitions from their targets and saved counters. Without any samples, the display can only estimate positions from segment weights and the saved duration. Race distance comparisons and final scoring remain separate from rail placement.

Validation: full application unit suite; five new regression cases covering unequal distance steps, recorded pacing changes, rest drift, mixed targets, legacy recordings, virtual pace targets, and regular/Pete's Plan race integration. Two emulator UI checks verify actual marker pixels and pause behavior using a separate application ID.
