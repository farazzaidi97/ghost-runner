# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

**Ghost Runner** — an audio-first running pacer. The user races against a virtual runner whose footsteps are spatialized in 3D audio based on the Δ between the user's actual pace and a target pace. Designed for screen-free running over BLE buds.

Two Android targets, one codebase:
- **`:app`** — Android Mobile, uses `FusedLocationProviderClient` + `Sensor.TYPE_STEP_DETECTOR`.
- **`:wear`** — Wear OS (standalone, runs in airplane mode), uses Health Services `ExerciseClient` for telemetry.
- **`:core`** — everything that is not platform-specific: domain models, pacing engine, Room schema, audio pipeline, pace formatting, the `PacingSnapshot` DTO that both UIs bind against.

The build plan that produced this codebase lives at `/Users/faraz/.claude/plans/you-are-a-lead-breezy-micali.md`. The active per-task checkpoint is `.claude/memory/checkpoint_ghost-runner-mvp.md` — read it first when picking work back up.

## Module structure

```
core/                      android-library, depended on by :app and :wear
  com.ghostrunner.core.
    domain/                PaceProfile, PaceState (sealed), LocationSample, CadenceSample
    engine/                PacingEngine, MovingAverageFilter, InertialFallback
    data/                  PaceProfileEntity / Dao / Database / Repository (Room)
    audio/                 SpatialAudioRenderer, FootstepSynthesizer, ChannelPanCalculator, AudioFocusController
    runtime/               PacingSnapshot (live state DTO observed by UIs)
    util/                  PaceFormatting (mps ↔ min/km, delta formatting)
  src/test/                JUnit tests for engine math (currently 19 tests)

app/                       android-application, mobile
  com.ghostrunner.app.
    GhostRunnerApplication, di/AppContainer
    core/                  LocationPipeline (Fused), StepDetectorSource, PacingEngineService (LifecycleService + foregroundServiceType=health)
    ui/                    MainActivity + Compose theme + NavGraph + ProfileList/Edit/Run screens

wear/                      android-application, Wear OS (standalone=true)
  com.ghostrunner.wear.
    GhostRunnerWearApplication, di/AppContainer (seeds 3 default profiles on first launch)
    core/                  HealthServicesPipeline (ExerciseClient), PacingEngineService (uses OngoingActivity)
    ui/                    MainActivity + WearTheme + NavGraph (SwipeDismissableNavHost) + ProfilePicker + RunControls
```

## Build commands

```bash
# Full sanity build
./gradlew clean :core:test :app:assembleDebug :wear:assembleDebug

# Iterate on a single module
./gradlew :core:test               # pacing engine unit tests
./gradlew :app:assembleDebug
./gradlew :wear:assembleDebug

# Lint
./gradlew :app:lintDebug :wear:lintDebug

# Install (requires emulator/device matching the module's profile)
./gradlew :app:installDebug        # Pixel API 34+ with Google Play Services
./gradlew :wear:installDebug       # Wear OS API 34 emulator
```

APK outputs land at `<module>/build/outputs/apk/debug/<module>-debug.apk`.

## Non-obvious decisions (read before changing)

- **KSP1, not KSP2** — KSP2 fails on Room 2.6.1 with `unexpected jvm signature V`. `gradle.properties` sets `ksp.useKSP2=false`. Don't flip it without verifying Room compiles.
- **Room and coroutines-core are `api()` deps in `:core`** — `:app` references `GhostRunnerDatabase` (subclass of `RoomDatabase`) and `Flow<T>` directly, so the types must be transitively visible.
- **Audio package lives in `:core`, not `:app`** — both platforms share `SpatialAudioRenderer` etc. The Android framework's `AudioTrack`/`AudioManager` are available in library modules.
- **`AudioTrack` 7.1 surround with stereo fallback** — uses `CHANNEL_OUT_7POINT1_SURROUND` (non-deprecated). If `getMinBufferSize` returns `ERROR_BAD_VALUE`, falls back to `CHANNEL_OUT_STEREO`. **Oboe / C++ / JNI / SCHED_FIFO are deferred** — the spec mentions them, but the MVP is pure Kotlin.
- **Footsteps are synthesized at runtime**, not bundled — `FootstepSynthesizer` builds an ~80 ms PCM click from filtered noise + exponential decay. No binary assets ship.
- **Footstep cadence is pegged at 180 spm** in both `PacingEngineService`s — one ghost footfall every ~333 ms. `PaceState` modulates channel gains, not BPM.
- **`AudioFocusController` requests `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` only on `PaceState.Behind`** — surrenders focus on OnPace/Ahead so background music plays at full volume until the ghost closes.
- **GPS reliability cutoff is 15 m accuracy** (`LocationSample.MAX_TRUSTED_ACCURACY_METERS`). Above that, `PacingEngine.onLocation` substitutes `cadence × stride` from the most recent `CadenceSample`. Falls back to raw GPS if no cadence yet.
- **`PaceState` epsilon is ±0.05 m/s** (`PaceState.ON_PACE_EPSILON_MPS`) to prevent thrashing when the user sits exactly on target.
- **Wear seeds 3 default profiles on first launch** because the wear UI has no editor in this MVP. Profiles are not synced phone↔watch — each Room DB is independent.
- **Wear depends on `kotlinx-coroutines-guava`** so Health Services `ListenableFuture` returns are resolvable. The async methods are called fire-and-forget; the data flow is driven by `ExerciseUpdateCallback`.
- **Mobile foreground service uses `ServiceCompat.startForeground(... FOREGROUND_SERVICE_TYPE_HEALTH)`.** Lint flags `InlinedApi` on the `0x100` constant because the type was added in API 34 while minSdk is 33 — `ServiceCompat` branches on the OS version internally, so this is safe to ignore.

## Testing

JUnit tests live in `core/src/test/kotlin/com/ghostrunner/core/engine/`:
- `MovingAverageFilterTest` — smoothing math, sliding window correctness.
- `PacingEngineTest` — state transitions across boundary, GPS-fallback path, reset.
- `InertialFallbackTest` — accuracy threshold + cadence×stride math.

19 tests total. Run with `./gradlew :core:test`. No instrumented tests in this MVP.

## Pinned versions (gradle/libs.versions.toml)

- AGP 8.7.3, Gradle wrapper 8.10.2, Kotlin 2.0.21 (K2), KSP 2.0.21-1.0.28 (KSP1 mode)
- Compose BOM 2024.10.01, Room 2.6.1, Play Services Location 21.3.0
- Health Services 1.1.0-alpha05 (no stable release exists yet), Wear Compose 1.4.0, Wear Ongoing 1.0.0
- compileSdk=35, targetSdk=35, minSdk=33, JVM toolchain 17

If you bump versions, prefer changing `gradle/libs.versions.toml` only — every module references it through the catalog.

## Checkpoint → compact → resume loop

This project runs long tasks across multiple compactions. To survive context summarization without losing state, follow this cadence **continuously until the task is done**:

### Maintain a per-task checkpoint file

For each task, keep a checkpoint at `.claude/memory/checkpoint_<task-slug>.md` with exactly these four sections:

```markdown
---
name: checkpoint_<task-slug>
description: Active checkpoint for <one-line task summary>
type: project
---

## Current task
<one-line description of what we're doing right now>

## Decisions & rationale
- <non-obvious choice made this session, with the why>
- ...

## Open questions / next steps
- [ ] <pending item>
- [ ] <pending item>

## Files touched
- `path/to/file.ext` — edited
- `path/to/other.ext` — pending edit
```

Add a pointer to this file in `.claude/memory/MEMORY.md` so post-compact sessions index it on load.

### When to refresh the checkpoint

Refresh after **whichever comes first**:
- A TODO item is completed.
- A non-trivial batch of edits lands.
- Roughly every ~10 tool calls.

This is the practical proxy for "≈20% context used" — you have no precise readout, so use these signals.

### When to ask the user to `/compact`

Trigger this sequence whenever you see a context-pressure system reminder, OR after a long uninterrupted run:

1. Refresh the checkpoint memory file.
2. Update `.claude/memory/MEMORY.md` if the index entry is missing or stale.
3. Emit one short message to the user: **"Context is heavy — please run /compact to checkpoint and continue."**
4. Stop and wait. Do not start new work.

After the user runs `/compact`:

1. Read the checkpoint file immediately.
2. Resume from the first unchecked item in **Open questions / next steps**.
3. Do **not** re-ask the user what we were doing — the checkpoint is the source of truth.

### Ground rules

- The checkpoint file is the source of truth across compactions. The compacted summary is supplementary.
- The model cannot invoke `/compact` itself — only the user can. Always prompt and wait.
- A `PreCompact` hook in `.claude/settings.json` writes timestamped markers to `.claude/memory/compaction_log.md` as a safety net, but it does **not** replace your responsibility to refresh the checkpoint before prompting `/compact`.
- After `/compact`, your first action is always to read the latest checkpoint. No exceptions.
