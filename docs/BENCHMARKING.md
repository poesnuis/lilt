# Benchmark procedure

Use [RESULTS.md](RESULTS.md) for actual observations. This document is a reproducible
procedure, not a claim that every listed run has already been performed.

## Controlled comparison

1. Use separate game directories for genuine vanilla 1.12.2, Forge alone, and
   Forge + Lilt. Record the full mod list. A disabled Lilt install is **not** a
   vanilla baseline. Use the same Java 8 build and explicit JVM memory settings.
2. Copy the same closed-world snapshot into each instance. Record world seed,
   dimension, coordinates, yaw/pitch, route, elapsed time and any fixture commands.
   Do not let one variant reuse another's warmed JVM or mutate its save.
3. Match resolution/fullscreen, render distance **16**, Fancy graphics, maximum
   smooth lighting, Fancy clouds, all particles, 4 mipmap levels, entity shadows,
   resource packs, FOV and VBO setting. Record the effective options file and
   server view distance. Keep external overlays and background programs consistent.
4. Disable VSync and use Minecraft's unlimited FPS setting **explicitly for the
   test**, then repeat latency/pacing tests with the user's preferred cap/VSync.
   Lilt itself never changes these options. Record refresh rate and presentation mode.
5. Warm up at least 120 seconds, including loading the route's chunks. Use cold
   traversal separately to test chunk loading. Capture at least 180 seconds per
   run and repeat at least three times in alternating/randomized order. Restore
   the world snapshot before each run. Report medians and run-to-run variability.

## Same observer on true vanilla and Lilt

The optional `lilt-benchmark-probe-1.0.0.jar` observes the start of Minecraft's main
game loop in both true vanilla and Forge. Put it outside `mods`. Add this **one JVM
argument** in your launcher, adjusting paths and the run label:

```text
-javaagent:/absolute/path/lilt-benchmark-probe-1.0.0.jar=output=/absolute/path/results;label=vanilla-survival-1;warmup=120;seconds=180
```

If paths contain spaces, quote the complete argument using your launcher's normal
JVM-argument syntax. On Windows, drive-letter paths are supported. Labels are
sanitized; use a unique label per run. Existing output files are never replaced.

The probe begins warming up after the first world frame with no GUI, then captures
for the configured duration. It records frame CSV and 1 Hz system CSV. Run the
same probe with `label=lilt-survival-1` in the Lilt instance, with matching settings.
This measures an instrumented baseline: both variants pay the probe's overhead.
It does not measure display presentation or input-to-photon latency.

```sh
python scripts/analyze.py results/vanilla-survival-1-frames.csv results/lilt-survival-1-frames.csv --output results/comparison.json
```

Normal in-client captures use Studio → Measurements or F8, writing to
`config/lilt/benchmarks`. They also save graphics/runtime metadata. Their hook is
Forge RenderTick START rather than main-loop entry, so use the external probe
for apples-to-apples vanilla comparisons. Do not combine unlike methods into one
performance claim. The optional probe is only for 1.12.2 and should be removed
from JVM arguments after testing.

## Required scene matrix

| Scenario | Suggested fixture | What it reveals |
|---|---|---|
| Empty world | Void preset; fixed platform, camera and time | Fixed CPU/UI overhead |
| Survival | Normal terrain; repeatable movement route and fixed save | Representative frame consistency |
| Complex terrain | Hills/forest/builds; both warmed route and cold traversal | Chunk rebuild/upload spikes |
| Multiplayer | Same server, player count, view distance and route | Client/network interaction under realistic load |
| Many entities | Explicit entity count/type/locations in a repeatable fixture | Entity render load; no entities culled/skipped |
| Extended play | 60–120 minutes, varied movement and resource reloads | Memory trends, GC and long-session stability |

Do not claim a multiplayer result from a singleplayer world. A local server is a
compatibility/controlled-load test, not an internet latency result. Camera-static
tests are distinct from traversal tests. Test online multiplayer using the user's
normal authenticated launcher/session.

## Metric definitions and limitations

- **Average FPS:** `N / sum(frame interval seconds)`, not the mean of instantaneous FPS.
- **1% low:** reciprocal of the mean of the slowest `ceil(N × 0.01)` intervals.
- **P50/P95/P99:** nearest-rank percentiles; population standard deviation is also
  reported. Raw intervals are retained, including long stalls.
- **Gameplay subset:** both ends of an interval must be in a world, and neither
  end may have a GUI open. Transition frames are excluded. Raw files retain them.
- **CPU:** process CPU-time delta as a fraction of all JVM-visible logical CPUs.
  A low percentage can still mean one saturated render thread. `-1` is unavailable.
- **Memory:** JVM heap used/committed/max. This is not whole-process resident memory
  or GPU memory; record RSS and GPU memory externally when reporting them.
- **GC:** MXBean collection count and cumulative collection time sampled at 1 Hz.
  This is not a histogram of individual stop-the-world pauses. For pause detail,
  use Java 8 `-Xloggc:<file> -XX:+PrintGCDetails -XX:+PrintGCDateStamps`, identically
  in all variants, or an appropriate external profiler.
- **GPU utilization:** not available from a portable LWJGL 2 API. Use vendor
  counters or an external system profiler and record its sampling rate.
- **Input responsiveness:** use a high-speed camera or calibrated hardware to
  measure physical input to displayed response. CPU frame time and server ping
  are not substitutes. No such result should be inferred from this recorder.

Record CPU/GPU model, driver, OS, Java build, RAM, JVM flags, power mode, thermals,
display resolution/refresh rate, world data, settings, mods, measurement method,
run durations, warmup and raw files. Shared cloud/software-rendered results must
be labeled as such and cannot establish a consumer GPU performance target.

## Text-width diagnostic

Run `/lilt fontcheck` in a test world. It checks 72 cases using the actual current
FontRenderer: null/empty, formatting and multilingual strings across Unicode and
bidirectional flag combinations. It then compares caching on/off using 128 repeated
scoreboard-style strings, 100,000 warmup calls and 500,000 timed calls per mode,
over five alternating-order rounds. Settings are restored in a `finally` block.

This explicit diagnostic briefly blocks the game thread; never run it during a
gameplay capture. It measures only text-width lookup reuse. It is not a JMH study,
nor evidence of a corresponding whole-game FPS multiplier.
