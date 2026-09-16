# Phase 1 workout identity and session lifecycle

The Gym now owns explicit session transitions: start, pause, resume, complete,
end early, and discard. Free Row starts a session without a Program and creates
its Workout when the first non-zero measurement arrives.

Pause keeps the rower and heart-rate connection open. Measurements received
while paused do not update workout totals, target progress, or snapshots. On
resume, cumulative changes reported by the rower during the pause are offset
from the remainder of the session.

Completion and early ending persist status and completion time before automatic
export is requested. Discard does not trigger export. End Session routes a
persisted workout to the initial Workout Complete surface and clears the active
Gym session.

Quick Start now creates exactly one Duration or Distance segment, carries the
Home shortcut's requested type into setup, and converts `/500 m` pace seconds to
the engine's centimeters-per-second speed value.
