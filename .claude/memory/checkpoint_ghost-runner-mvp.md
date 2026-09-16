---
name: checkpoint_ghost-runner-mvp
description: Active checkpoint for Ghost Runner MVP — Android Mobile + Wear OS pacer build
type: project
---

## Current task
**MVP complete.** All 12 plan steps shipped. Both APKs build clean (`./gradlew clean :core:test :app:assembleDebug :wear:assembleDebug` green). 19/19 unit tests pass. 0 lint errors. Awaiting manual emulator smoke from user (can't run from this context).

## Decisions & rationale
- **Three-module layout** (`:core`, `:app`, `:wear`). Shared everything-not-platform-specific lives in `:core`: domain, engine, Room schema, audio pipeline, pace formatting, runtime snapshot type. Both Android platforms depend on `:core`.
- **KSP1, not KSP2:** KSP2 fails with `unexpected jvm signature V` against Room 2.6.1. Set `ksp.useKSP2=false`.
- **Room (`runtime`) and `kotlinx-coroutines-core` are `api()` deps in `:core`** so consumer modules can resolve `RoomDatabase` (supertype of `GhostRunnerDatabase`) and `Flow<T>` (return type of repository methods).
- **Audio pipeline lives in `:core`** (`com.ghostrunner.core.audio`). The Android framework `AudioTrack`/`AudioManager`/`AudioFocusRequest` types are available to library modules. Wear and phone services both instantiate the same `SpatialAudioRenderer`.
- **Manual `AppContainer` DI** per platform — each holds a singleton DB, repository, and `MutableStateFlow<PacingSnapshot?>` that the foreground service writes to and the UI observes via `StateFlow`. No Hilt.
- **Spatial audio: AudioTrack 7.1 surround + stereo fallback.** Tries `CHANNEL_OUT_7POINT1_SURROUND` first (non-deprecated variant). If `getMinBufferSize` returns ERROR, falls back to `CHANNEL_OUT_STEREO` with constant-power-style panning. Oboe / C++ / JNI deferred to a later iteration.
- **Footstep audio is synthesized procedurally** in `FootstepSynthesizer` — lowpass-filtered white noise + exponential decay envelope, ~80 ms mono 16-bit PCM. Generated once at renderer init, interleaved per-channel with state-derived gains for every `enqueueFootstep`. Zero binary assets shipped.
- **`AudioFocusController.applyForState`:** requests `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` only when state is `Behind`; abandons focus on `OnPace`/`Ahead`. Implements spec §3.4's directional ducking.
- **Footstep cadence fixed at 180 spm** in both `PacingEngineService` impls — one ghost footfall every ~333 ms; gains modulate by `PaceState`, not BPM.
- **Foreground service uses `LifecycleService` + `ServiceCompat.startForeground(... FOREGROUND_SERVICE_TYPE_HEALTH)`.** API 34+ requires typed foreground services for the health type. The InlinedApi lint warning is benign on minSdk=33 because ServiceCompat handles version branching.
- **Pacing engine epsilon = ±0.05 m/s** for `OnPace`. Outside the band → `Behind` (Δ>0) or `Ahead` (Δ<0). Prevents state thrashing.
- **GPS reliability cutoff at 15 m accuracy** (spec §5.1). When `LocationSample.accuracyMeters > 15` (or negative), `PacingEngine.onLocation` substitutes `cadence × strideMeters` from the last `CadenceSample`. Falls back to raw GPS if no cadence yet.
- **Wear seeds 3 default profiles on first launch** (Easy 6:00/km, Tempo 5:00/km default, Race 4:30/km) — wear has no built-in profile editor in this MVP, so seeding gives a usable picker. Mobile lets you CRUD profiles freely.
- **Wear uses `OngoingActivity`** (not just a notification) so the foreground status surfaces in Wear tile/ambient.
- **Wear adds `kotlinx-coroutines-guava`** to make `ListenableFuture` (returned by Health Services async methods) resolvable on the classpath. Methods are called fire-and-forget; the callback registered via `setUpdateCallback` is what drives the data flow.
- **Versions pinned (final):** AGP 8.7.3 / Gradle wrapper 8.10.2 / Kotlin 2.0.21 (K2 on) / KSP 2.0.21-1.0.28 (KSP1 mode) / Compose BOM 2024.10.01 / Room 2.6.1 / Play Services Location 21.3.0 / Health Services 1.1.0-alpha05 / Wear Compose 1.4.0 / Wear Ongoing 1.0.0. compileSdk=35, targetSdk=35, minSdk=33, JVM toolchain 17.

## Open questions / next steps
All 12 plan steps complete:
- [x] Step 1 — Gradle scaffolding.
- [x] Step 2 — `:core` domain + Room.
- [x] Step 3 — Pacing engine + 19 JUnit tests passing.
- [x] Step 4 — `:app` foundation (manifest §4, Application, MainActivity, Compose theme).
- [x] Step 5 — `LocationPipeline` + `StepDetectorSource`.
- [x] Step 6 — Audio pipeline (then later moved to `:core`).
- [x] Step 7 — `PacingEngineService` mobile foreground.
- [x] Step 8 — Compose UI on mobile (ProfileList/Edit/Run).
- [x] Step 9 — `:wear` foundation (standalone manifest, Application, MainActivity, theme).
- [x] Step 10 — `HealthServicesPipeline`.
- [x] Step 11 — wear `PacingEngineService` w/ `OngoingActivity`, `ProfilePickerScreen`, `RunControlsScreen`.
- [x] Step 12 — Verification: clean build green, 0 lint errors.

Outstanding (deferred per scoping or out of scope for MVP):
- [ ] Manual emulator smoke (mobile Pixel API 34 + Wear OS API 34) — user must run from their machine.
- [ ] C++ Oboe / JNI / SCHED_FIFO real-time audio engine.
- [ ] Wearable Data Layer phone↔watch sync (each platform standalone right now).
- [ ] Per-sample GPS accuracy extraction from Health Services LOCATION data (currently uses fixed 5 m assumed-accuracy on wear).
- [ ] Profile editor on the watch (mobile-only for now).
- [ ] Thermal throttling detection + downsampling.
- [ ] Complications, run history, PRs.

## Files touched
**Root:** `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `local.properties`, `.gitignore`, `gradlew*`, `gradle/wrapper/*`

**`:core`** — `build.gradle.kts`, `consumer-rules.pro`, `src/main/AndroidManifest.xml`
- `domain/`: `PaceProfile.kt`, `PaceState.kt`, `LocationSample.kt`, `CadenceSample.kt`
- `engine/`: `MovingAverageFilter.kt`, `InertialFallback.kt`, `PacingEngine.kt`
- `data/`: `PaceProfileEntity.kt`, `PaceProfileDao.kt`, `GhostRunnerDatabase.kt`, `PaceProfileRepository.kt`
- `audio/`: `FootstepSynthesizer.kt`, `ChannelPanCalculator.kt`, `SpatialAudioRenderer.kt`, `AudioFocusController.kt`
- `runtime/`: `PacingSnapshot.kt`
- `util/`: `PaceFormatting.kt`
- `src/test/`: `MovingAverageFilterTest.kt`, `InertialFallbackTest.kt`, `PacingEngineTest.kt` (19 tests)

**`:app`** — `build.gradle.kts`, `proguard-rules.pro`, `AndroidManifest.xml`, `res/values/{strings,themes}.xml`, `res/drawable/{ic_launcher_foreground,ic_notification}.xml`, `res/mipmap-anydpi-v26/ic_launcher.xml`
- `GhostRunnerApplication.kt`, `di/AppContainer.kt`
- `core/`: `LocationPipeline.kt`, `StepDetectorSource.kt`, `PacingEngineService.kt`
- `ui/`: `MainActivity.kt`, `theme/{Color,Type,GhostRunnerTheme}.kt`, `navigation/NavGraph.kt`, `screens/{ProfileList,ProfileEdit,Run}Screen.kt`

**`:wear`** — `build.gradle.kts`, `proguard-rules.pro`, `AndroidManifest.xml`, `res/values/{strings,themes}.xml`, `res/drawable/{ic_launcher_foreground,ic_notification}.xml`, `res/mipmap-anydpi-v26/ic_launcher.xml`
- `GhostRunnerWearApplication.kt`, `di/AppContainer.kt` (with seedDefaultsIfEmpty)
- `core/`: `HealthServicesPipeline.kt` (+ `HealthSample` sealed class), `PacingEngineService.kt`
- `ui/`: `MainActivity.kt`, `theme/WearTheme.kt`, `navigation/NavGraph.kt`, `screens/{ProfilePicker,RunControls}Screen.kt`

**Built artifacts:** `app/build/outputs/apk/debug/app-debug.apk` (58 MB), `wear/build/outputs/apk/debug/wear-debug.apk` (28 MB).
