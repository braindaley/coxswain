# Dark-mode visual refinement

Visual review found a pervasive blue cast, bright decorative separators, competing repeated primary buttons, selected segmented tabs darker than their track, incomplete Material surface roles, and native Settings colors that differed from Compose.

The dark scheme now uses neutral slate surfaces with explicit page/card/control/dialog levels. Body text is a softer off-white; secondary text and blue accents retain readable contrast. Decorative separators are quieter, while interactive input outlines retain at least 3:1 contrast. Selected segmented tabs use a raised surface. Program-list Start buttons use a subdued tonal treatment so the main action remains prominent. Pete's Plan's dark header and native Settings use the same palette; Material tertiary/inverse/surface roles are explicitly assigned to avoid unrelated defaults.

Light colors and the specialized live-row/heart-rate zone palettes remain intact. Palette selection follows the app's theme preference, including when it differs from the system theme.

Validation includes emulator screenshots of Home, Programs, workout results and Settings; contrast assertions for text, buttons, selected controls, dialogs and input outlines; existing live heart-rate tile checks; and the application unit suite. One initial unit run encountered a database-lock failure in the unchanged audio-default test; the subsequent complete run passed. Screenshot capture also needed an activity-transition wait. Emulator-only visual fixtures were used; the user's phone preferences/history were not modified.
