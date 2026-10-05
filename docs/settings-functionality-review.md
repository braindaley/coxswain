# Settings functionality review — 2026-10-04

## Changes

- Disable external database switching: the old control changed databases without migrating history and reset active-session state. Existing storage selection is preserved. Migration remains a separate feature before this control can return.
- Reject nonpositive numeric settings and weights outside the calorie adjuster's supported 40–160 kg range. Clamp legacy retention values to at least one day before deleting snapshots.
- Reset both legacy display preferences and the current Live Row layout.
- Open HealthConnectManageActivity from Settings, exposing application permission status and grant controls.
- Remove shared-storage permission requests for app-private diagnostics; export a bounded logcat snapshot on a background thread and report completion only after success.
- Preserve Settings/Devices fragments on activity recreation and ringtone-picker routing across recreation.
- Explain reconnection requirements for measurement adjustments, heart-rate source and hardware diagnostics. Align weight fallback with the 68 kg default.
- Hide legacy controls with no consumers in the current screens: units, split distance, number formatting, picture-in-picture, program-start intent, and optional completion results. Current design continues to use meters, kcal and /500 m and always presents completion results. These features were not reimplemented.

## Verification

Debug APK builds. Existing unit suite and the new Settings interaction regression pass (96 tests). The new test checks Live Row reset, disabled storage switching, retention validation and supported weight boundaries. Hardware reconnects, ringtone picker UI, system Health Connect permissions and device diagnostic files still require physical-device checks.

## Scope limits

Automatic export still requires a previously configured export destination (or enabling Health Connect through More). Storage migration and the hidden legacy features are not implemented by this repair. No on-device end-to-end verification is claimed.
