# Phase 0 validation baseline

Phase 0 establishes a repeatable build and test boundary before the product-design
implementation changes application behavior or persisted workout data.

## Automated checks

GitHub Actions and local validation use the same Gradle command:

```shell
./gradlew --no-daemon :app:assembleDebug testDebugUnitTest :app:assembleDebugAndroidTest
```

This command verifies the debug application build, all JVM unit tests, and
compilation and packaging of the Android instrumentation suite. The resulting
application and test APKs are uploaded by CI.

The instrumentation suite contains an application-context smoke test and a
Compose navigation test covering Home, Programs, History, and More. Running those
tests requires an API 34 or newer emulator or physical device; Phase 0 compiles
and packages them so a device-backed CI job can be added independently.

## Legacy test boundary

The legacy Propoid tests now compile against current AndroidX and Robolectric
APIs. Two LoaderManager callback suites remain ignored because their asynchronous
callbacks do not complete deterministically on Robolectric 4.14:

- `MatchAdapterTest`
- `ReferenceLookupTest`

Their source and assertions remain in place. The rest of the unit suite runs on
every push and pull request.

## Protected behavior

Focused tests cover program and free-form segment behavior, workout measurement
updates, Gym progress and segment transitions, Health Connect record conversion,
and loading the frozen database-v1 migration fixture.
