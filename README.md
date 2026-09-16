# Ghost Runner

An audio-first running pacer. You race a virtual runner whose footsteps are
spatialized around you in 3D, positioned by the difference between your actual
pace and your target pace. No screen, no glancing at your wrist — you hear
where you stand.

Built for running with BLE earbuds. Two Android targets, one shared codebase.

> **Status:** MVP. Both modules build clean and the pacing engine is unit-tested,
> but the app has not yet been smoke-tested on a physical device or emulator.
> See [Known gaps](#known-gaps).

## How it works

Every GPS fix produces a speed, which is smoothed over a 3-sample moving
average. The engine computes:

```
Δ = targetSpeed − smoothedSpeed
```

and maps it to one of three states, with a ±0.05 m/s dead zone around the
target so the state doesn't thrash when you sit right on pace:

| State    | Meaning                     | Where the footsteps sit           |
| -------- | --------------------------- | --------------------------------- |
| `Behind` | Δ > +0.05 — you're slower   | Rear channels (BL/BR)             |
| `OnPace` | \|Δ\| ≤ 0.05                | Balanced, side channels dominant  |
| `Ahead`  | Δ < −0.05 — you're faster   | Front channels (FL/FR/FC)         |

Within a state, the gap size sets the volume:

```
proximity = clamp(1 − |Δ| / 2.0, 0.05, 1.0)
```

The ghost is loudest when you're near your target and fades as the gap widens —
so closing on pace sounds like the ghost drawing alongside you.

When you fall behind, and only then, `AudioFocusController` requests
`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` to duck your music. Back on pace, it
surrenders focus and your music returns to full volume.

Footsteps fire at a fixed 180 spm (one footfall every ~333 ms) on both
platforms. Pace state modulates channel gains, not tempo.

### When GPS gets unreliable

Above 15 m reported accuracy, the GPS speed is discarded and the engine
substitutes `cadence × strideLength` from the most recent step-detector sample.
If no cadence has arrived yet, it falls back to the raw GPS value rather than
stalling.

### Audio output

`SpatialAudioRenderer` opens an `AudioTrack` on `CHANNEL_OUT_7POINT1_SURROUND`.
If `getMinBufferSize` rejects that layout, it falls back to stereo panning and
flags it on the live snapshot. Footsteps are synthesized at runtime — an ~80 ms
PCM click built from lowpass-filtered noise and an exponential decay envelope —
so no binary audio assets ship with the app.

## Modules

```
core/    Android library — everything not platform-specific
         domain/   PaceProfile, PaceState, LocationSample, CadenceSample
         engine/   PacingEngine, MovingAverageFilter, InertialFallback
         data/     Room entity, DAO, database, repository
         audio/    SpatialAudioRenderer, FootstepSynthesizer,
                   ChannelPanCalculator, AudioFocusController
         runtime/  PacingSnapshot — the DTO both UIs bind against
         util/     PaceFormatting (m/s ↔ min/km)

app/     Android Mobile
         FusedLocationProviderClient + Sensor.TYPE_STEP_DETECTOR,
         LifecycleService foreground service, Compose UI with full
         profile CRUD

wear/    Wear OS, standalone (runs in airplane mode)
         Health Services ExerciseClient for telemetry,
         OngoingActivity for ambient/tile surfacing,
         Wear Compose UI with a profile picker
```

Both platform modules depend on `:core` and each own a manual `AppContainer`
for DI — a singleton database, repository, and a `MutableStateFlow<PacingSnapshot?>`
that the foreground service writes and the UI observes. No Hilt.

Profiles are **not** synced between phone and watch. Each holds an independent
Room database. Because the watch has no profile editor in this MVP, it seeds
three defaults on first launch: Easy 6:00/km, Tempo 5:00/km (default), and
Race 4:30/km.

## Building

Requires JDK 17 and the Android SDK. Point `local.properties` at your SDK:

```properties
sdk.dir=/path/to/android-sdk
```

Then:

```bash
# Full sanity build
./gradlew clean :core:test :app:assembleDebug :wear:assembleDebug

# Single module
./gradlew :core:test           # pacing engine unit tests
./gradlew :app:assembleDebug
./gradlew :wear:assembleDebug

# Lint
./gradlew :app:lintDebug :wear:lintDebug

# Install
./gradlew :app:installDebug    # Pixel API 34+ with Google Play Services
./gradlew :wear:installDebug   # Wear OS API 34 emulator
```

APKs land at `<module>/build/outputs/apk/debug/<module>-debug.apk`.

## Testing

19 JUnit tests in `core/src/test/kotlin/com/ghostrunner/core/engine/`:

- `MovingAverageFilterTest` (6) — smoothing math and sliding-window correctness
- `PacingEngineTest` (8) — state transitions across the epsilon boundary, the
  GPS-fallback path, and reset
- `InertialFallbackTest` (5) — accuracy threshold and cadence × stride math

Run with `./gradlew :core:test`. There are no instrumented tests in this MVP;
the platform pipelines and audio renderer are currently unverified by automation.

## Versions

Pinned in `gradle/libs.versions.toml` — prefer editing only that file, since
every module resolves through the catalog.

| | |
| --- | --- |
| AGP / Gradle | 8.7.3 / 8.10.2 |
| Kotlin | 2.0.21 (K2) |
| KSP | 2.0.21-1.0.28, **KSP1 mode** |
| Compose BOM | 2024.10.01 |
| Room | 2.6.1 |
| Play Services Location | 21.3.0 |
| Health Services | 1.1.0-alpha05 |
| Wear Compose / Ongoing | 1.4.0 / 1.0.0 |
| SDK | compile 35, target 35, min 33 |
| JVM toolchain | 17 |

`gradle.properties` sets `ksp.useKSP2=false`. KSP2 fails against Room 2.6.1 with
`unexpected jvm signature V` — don't flip it without verifying Room still
compiles.

## Permissions

Mobile requests fine/coarse location, activity recognition (step detector),
foreground service + health type, wake lock, audio settings, Bluetooth connect,
and notifications. Wear swaps the step detector for body sensors and drops the
GPS feature requirement, since Health Services supplies telemetry.

## Known gaps

Deferred from the MVP, roughly in priority order:

- Manual emulator/device smoke test — **not yet done**
- C++ Oboe / JNI / `SCHED_FIFO` real-time audio path (currently pure Kotlin)
- Wearable Data Layer phone ↔ watch profile sync
- Per-sample GPS accuracy from Health Services — wear currently assumes a fixed 5 m
- Profile editor on the watch
- Thermal throttling detection and downsampling
- Complications, run history, personal records

## License

No license file yet — all rights reserved by default until one is added.
