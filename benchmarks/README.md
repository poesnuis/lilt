# Recorded lab evidence

These are genuine Minecraft 1.12.2 software-renderer measurements collected on
11 November 2026. See `../docs/RESULTS.md` for interpretation and untested scope.

- `*-coast-*-frames.csv`: matching external Java-agent loop-entry observations on
  true vanilla or Forge + Lilt. Every run used a fresh process and a fresh copy
  of the same closed-world snapshot. Run order alternated vanilla/Lilt three times.
  A final Forge-only run is an exploratory control, with one sample rather than three.
- `*-coast-*-system.csv`: 1 Hz Java heap, process CPU and cumulative GC counters.
  The first CPU sample is unavailable (`-1`), rather than an invented zero.
- `comparison.json`: frame statistics and system summaries derived from those
  CSVs. GC deltas cover the first-to-last system sample, which is slightly shorter
  than the complete frame capture. CPU percentages use JVM-visible processors.
- `20261111-171553-820-*`: separate 60-second internal recorder functional check
  with four visible HUD modules. Its `Survival` label is the default F8 label;
  the fixture itself is a creative-mode, fixed-camera coastal scene.
- `font-check.json`: all 72 real-font parity checks passed; five raw lookup timing
  rounds. These timings are not whole-game speedups.
- `options.txt`, `fixture.json`, `fixture-world.zip`, `provenance.json` and
  `hardware-cpu.json`: settings, saved-world metadata/hashes, generated fixture,
  run/build provenance and host CPU identification.

## Reproduce the short comparison

1. Create separate normal launcher instances for genuine vanilla 1.12.2 and
   Forge 14.23.5.2860 + Lilt. Use your own authenticated account and 64-bit Java 8.
2. With the game closed, extract `fixture-world.zip` into each instance's `saves`
   directory. Its top-level folder is `Lilt Lab`. Restore it before each run.
   The local lab used a synthetic development identity; on another account,
   verify the camera/position after joining against `fixture.json` before timing.
3. Apply the graphics settings in `options.txt` explicitly to both instances.
   Keep unrelated personal keybinds if editing an existing options file. The lab
   disabled lost-focus pausing and tutorial toasts for automation. Use 1440×900,
   windowed, default resources, 70° FOV, and `-Xms512M -Xmx2G` in both instances.
4. Add the same optional probe to each launcher's JVM arguments, outside `mods`:

   ```text
   -javaagent:/absolute/path/lilt-benchmark-probe-1.0.0.jar=output=/absolute/path/results;label=vanilla-coast-1;warmup=60;seconds=30
   ```

5. Join the saved fixture, leave the camera stationary and keep menus closed.
   Use a fresh output label each time. Lilt uses its default profile: FPS HUD and
   text cache enabled, other modules disabled. Alternate variants three times.
6. Run `python scripts/analyze.py /path/to/results/*-frames.csv --output summary.json`
   from the extracted project/release root. The standard-library analyzer uses
   the same definitions for both variants.

The fixture uses normal terrain generator seed **20261110**, position
**8.5, 90, 8.5**, yaw **135°**, pitch **20°**, looking across ocean and shoreline.
A stone block at 8,89,8 holds the stationary creative-mode player. Time is fixed
at 6000, weather is clear and new mob spawning is disabled using vanilla fixture
commands. Existing entities remain. These explicit test-fixture choices are
identical for both variants; Lilt itself does not change world rules or entities.

This is a **short static lab comparison**, not the full benchmark protocol.
Sixty seconds of warmup may still include chunk/render/JIT work on this slow
software renderer. The 30-second samples contain only a few hundred frames, so
their 1% tail contains only a few intervals. The shared CPU is uncontrolled, and
the single Forge-only control is insufficient for reliable causal attribution.
Use longer warmups/captures and the scene matrix in `../docs/BENCHMARKING.md` for
performance conclusions. A physical GPU and external instruments are needed
for the target FPS, GPU utilization and physical-input latency acceptance work.
