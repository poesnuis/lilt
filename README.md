# Lilt Client · Minecraft 1.12.2

A client-only Forge package with a configurable HUD, an original Studio interface,
bounded font-metric caching, and transparent performance measurement. Java 8 and
LWJGL 2 throughout. Vanilla world simulation, movement, combat and networking are
left intact.

**240+ FPS at 16 chunks and maximum graphics is a target, not a demonstrated or
guaranteed result.** See [measured results](docs/RESULTS.md) for the exact validation
environment and remaining coverage. Lilt 1.0 does not replace Minecraft's terrain
renderer or include unvalidated chunk-rebuild patches.

## Install

1. In the official Minecraft launcher or your existing launcher, install and run
   Minecraft Java Edition **1.12.2** using your own licensed account.
2. Install [Forge 14.23.5.2860](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html)
   for that instance. Select a **64-bit Java 8** runtime. Java 17/21 are not supported.
3. Close Minecraft. Put `lilt-client-1.12.2-1.0.0.jar` in that instance's `mods`
   directory. Do not install the sources JAR or benchmark probe as mods.
4. Launch the Forge instance. Use **Right Shift** in a world, or the **Lilt Studio**
   button on the main/pause menu. Use **F9** to arrange the HUD.

Optional copy helper, with Python 3.8+: `python scripts/install.py "/path/to/game-directory"`.
It only copies Lilt; it does not install Java, Minecraft or Forge, edit launcher
accounts, or modify game options. Existing same-name JARs are backed up.

Use a separate game directory when evaluating alongside other modpacks. To remove
Lilt, remove its JAR. Profiles remain in `config/lilt` for later use.

## What is implemented

- Frame rate, frame-time graph, coordinates/facing, Java heap/GC, vanilla player-list
  latency, session time, local clock, input display and a small signature.
- Per-module enable state, category, description and optional keyboard shortcut.
- Dragging, resizing, keyboard nudging and optional grid snapping. Positions adapt
  to resolution and GUI-scale changes. Compact, Balanced and Focus layouts.
- Per-module opacity, background, rounded corners, border, padding, line spacing,
  scale, font size, weight, alignment, accent and text/background/border HEX colors.
- Open Sans and Minecraft fonts. Dusk, Daylight and Paper HUD presets; custom named
  themes and Studio accent colors stored in profiles. Optional fade-in and speed.
- Debounced background saves, named profiles, import/export, bounded input,
  validation, backups and recovery from corrupt configuration.
- Bounded, per-thread vanilla text-width cache with reload/flag invalidation and
  an immediate disable switch. No packet changes or game-tick hooks.
- In-game CSV/JSON recorder plus a separate optional Java agent for using the same
  frame observer with true vanilla and Lilt. Python analysis requires no libraries.

Only the FPS module and text cache are enabled initially. **No graphics setting,
render distance, FPS limit, VSync setting, entity update or particle update is
changed automatically.** In Minecraft 1.12.2 there is no separate vanilla
simulation-distance setting.

## Controls

| Action | Default |
|---|---|
| Studio | Right Shift; main/pause menu button; `/lilt` |
| HUD editor | F9 |
| Start/stop 60-second capture | F8 or `/lilt capture` |
| Stop a capture | `/lilt stop` |
| Explicit font parity/microbenchmark diagnostic | `/lilt fontcheck` |
| Move module in editor | Drag; arrows; Shift+arrows = 10 px |
| Resize module in editor | Bottom-right handle or mouse wheel |
| Snap while dragging | Hold Shift |
| Cycle enabled modules | Tab in editor |
| Hide selected module | Delete in editor |
| Finish editing | Escape, Enter or Done |

The three global keybinds can be changed in Minecraft's Controls screen. Module
shortcuts are set in Studio → Modules. Avoid assigning a shortcut that is already
used by the game or another module. Input-display W/A/S/D labels indicate movement
**actions**; activation respects your actual Minecraft key bindings. L/R indicate
physical mouse buttons.

## Build from source

Extract the separate source archive if you downloaded the binary release.
Install a 64-bit JDK 8 and set `JAVA_HOME` to it. In the source directory:

```sh
./gradlew --no-daemon clean build
python3 -m unittest discover -s scripts -p 'test_*.py'
./gradlew runClient
```

On Windows use `gradlew.bat`. The Gradle 4.9 wrapper verifies its distribution
checksum. ForgeGradle, Forge and MCP mappings are pinned. The build pins the
legacy merge-tool compatibility dependency to avoid the known 1.12.2 `BUKKIT`
enum startup conflict. Initial dependency downloads need internet access to
Maven Central, Minecraft/Mojang, Forge and Gradle distribution hosts.

Artifacts are written to `build/libs`. The client JAR is reobfuscated for real
Forge installations; the separate sources JAR is for inspection. The optional
`lilt-benchmark-probe-1.0.0.jar` is a Java agent, not an additional gameplay mod.
If ForgeGradle's first asset download reports a duplicate-icon hash failure,
rerun `runClient`; both asset entries reference the same official file.

## Documentation

- [Configuration and theme format](docs/CONFIGURATION.md)
- [Architecture and optimization rationale](docs/ARCHITECTURE.md)
- [Benchmark procedure and metric definitions](docs/BENCHMARKING.md)
- [Measured results and validation coverage](docs/RESULTS.md)
- [Compatibility and limitations](docs/COMPATIBILITY.md)

Lilt's original code is MIT licensed. Open Sans is Apache 2.0; the optional probe
bundles ASM 5.2 under its BSD license. Notices and license texts are included.
Minecraft, its assets, Forge, Java and user authentication are not bundled in the
release. Lilt is an independent project, unaffiliated with Mojang/Microsoft,
Lunar Client or Badlion Client.
