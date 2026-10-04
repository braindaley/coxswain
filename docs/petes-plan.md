# Pete's Plan implementation

The app includes the supplied 24-week beginner schedule: three required and two optional full sessions each week. The introduction card appears at the bottom of Home before enrollment; the active-week card appears above Last workout after enrollment. The overview includes all weeks, completed-session checks, estimated session times with an adjustable estimate pace, and weekly meters/time charts.

Weeks run Sunday–Saturday in the local timezone. Three completed required sessions allow automatic advancement on Sunday. An incomplete week pauses for a move-on or repeat decision. Repeating creates a fresh attempt; stopping the plan removes participation state and returns Home to the introduction card. Completed workouts remain in History. A new enrollment receives a new identity and cannot inherit completion from a stopped enrollment.

Only completed sessions supply plan credit and coaching references. Workouts save enrollment, week, attempt, session index, frozen goal, source timestamp, and rowing-only time/distance/strokes. Database version 5 and JSON backup/restore include these fields. Prescribed rests and pauses do not advance a constant-pace opponent or dilute the saved rowing-only split. Plan sessions automatically finish at the full workout target.

## Coaching behavior

`petes_plan.json` preserves the schedule and original coaching text. `petes_plan_rules.json` contains an explicit interpretation for each of the 120 notes. Runtime code does not parse prose. Exact same-pace references and explicit offsets yield split targets; instructions to beat a recorded row can select replay; rate caps use an upper-bound comparison. Qualitative and conditional instructions remain guidance rather than invented numeric offsets. Missing references leave the session available without a calculated goal.

A source review corrected the Week 4 optional interval to reference the optional 6,000 m session, added the Week 2 optional fallback, added Week 12's first-two-piece pace goal, corrected the latest 4 × 2,000 m selector, and made Week 23's 30-minute reference use the best completed fixed-duration result. Week 14's optional schedule says seven 500 m pieces while its note describes seven opening pieces and a faster final piece; the full prescribed schedule is retained, with the goal applied to the first six and the original note preserved.

## Validation and remaining work

Automated integration coverage includes the full catalog, interval rest accounting, incomplete-session exclusion, named reference resolution, stop/re-enroll history preservation, explicit optional fallbacks, and best-duration selection. The debug build and full unit suite are the release checks for this checkpoint.

Device testing with a real rower remains necessary. Advanced coaching still needs explicit per-piece result records and changing target profiles (such as beating the first piece's distance during the second), target overrides, comparison selection when both a coaching target and recorded best are available, and historical coaching presentation. References that require comparing two prior session types currently show one pacing reference plus the original comparison guidance. These are follow-up items; the current implementation is a testing checkpoint rather than a claim that every coaching instruction is automated.
