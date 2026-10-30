# Pete's Plan row credit and split labels

Live split is the recent five-second display average. Work average split is calculated across all work intervals so far, excluding rest and pauses. The live tile/picker labels now distinguish those meanings. Both remain available in customized layouts; the goal comparison applies to live split. Five-second split display averaging now also applies when split is displayed without being the active goal.

Plan Home, week cards, week detail and selected-week progress summaries now show all five prescribed rows, with separate required and optional counts. Week-card progress bars represent completed rows out of five. The three required rows still determine advancement. Optional rows remain visible on Home while reviewing an ended week or a finished plan. The post-workout recap also shows the relevant enrollment/week/attempt's required and optional credit.

Completion is deduplicated by session index and excludes incomplete rows and other enrollments/attempts. Weekly training totals continue including optional work as before. Existing history/preferences do not need migration.

Regression coverage includes all-five-row Home/overview/recap displays, custom live split tile labels/feedback, and counting duplicate, incomplete, other-attempt and other-enrollment rows. Validation also exposed an unrelated audio-default test teardown that cleared storage preferences and reopened the database; that test now changes/restores only its three audio preferences.
