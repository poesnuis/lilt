# Architecture and performance rationale

Lilt is a thin, client-only Forge 14.23.5.2860 mod plus one narrow font transformer.
It does not redistribute Minecraft classes. The user retains their existing
launcher, account and installation. All original source targets Java 8.

```text
dev.lilt.LiltClient         Forge lifecycle, event dispatch, keybinds
dev.lilt.module            Immutable module metadata
dev.lilt.config            Validated model and background persistence
dev.lilt.ui                Glyph atlases, drawing, Studio and HUD editor
dev.lilt.metrics           Frame statistics, system sampling, capture writer
dev.lilt.core              Optional bounded FontRenderer cache
src/probe                  Separate vanilla-compatible benchmark Java agent
scripts/                   Installation and consistent CSV analysis
```

## Optimization boundaries

The starting hypothesis is that repeatedly walking formatted text can waste CPU
in scoreboards, labels and menus. Minecraft 1.12.2's `FontRenderer.getStringWidth`
walks characters and formatting codes. It has no dependencies on world ticking
or GL state. This makes it a much narrower optimization target than chunk
rebuild scheduling, model mutation, render uploads or entity update frequency.
The actual font diagnostic measures that hypothesis using the installed font.
It does **not** establish that font metrics dominate a particular gameplay scene.

The transformer adds a lookup before `getStringWidth(String)` and memoizes its
normal return value. There is no replacement width implementation. Each thread
has an access-ordered 512-entry cache, tied to the current renderer identity by
a weak reference. Null strings, negative results, and strings over 128 UTF-16
code units bypass it. A single epoch invalidates caches after resource reloads
or changes to Unicode/bidirectional flags. Disabling the module immediately
bypasses reads and writes. Missing or unexpected target signatures leave the
original class untouched. Installation status is visible in Modules.

The transformer touches exactly one class. It preserves existing stack-map
frames and computes only maximum stack sizes, avoiding Minecraft class loading
while LaunchWrapper is transforming classes. Unit tests load and execute a
transformed fixture to check JVM verification and invalidation. `/lilt fontcheck`
checks actual vanilla font results across formatted, multilingual and empty/null
inputs, then performs an explicit repeated-string microbenchmark.

## Work owned by Lilt

- One Forge listener dispatches client ticks, render ticks, overlay drawing and
  key events. There is no reflective event bus per module.
- Frame intervals use a fixed primitive ring; the graph reads it without copying.
  Rolling summaries sort at most 240 values once per second.
- Captures allocate bounded primitive arrays once on start (about 9 MB for one
  million intervals and flags), then append without per-frame allocation.
  Sorting, statistics and CSV/JSON writes happen after the detached capture is
  handed to the writer. Only one capture is recorded/written at a time.
- System counters are sampled once per second. HUD strings update at 4 Hz, while
  input indicators sample held input during rendering. Player/world reads stay
  on the Minecraft thread.
- Font atlases are two 512×512 RGBA textures, generated/uploaded once on the
  render thread. Open Sans handles Latin-1; other scripts use Minecraft's font.
  UI glyphs are batched per label. The frame graph and editor grid each use one
  draw call. Fixed arc tables avoid per-corner trigonometry.
- Interface drawing saves/restores the GL enable, texture and blend state it
  changes. GL work is never moved to worker threads. Opacity uses nonoverlapping
  border/fill geometry.
- Settings are snapshotted before dispatching background IO. Debouncing combines
  slider edits, and profile switching orders saves and loads on one worker.

The settings snapshot and low-frequency HUD string formatting still allocate.
The whole client is not claimed to be allocation-free. The Studio creates its
small control list only while open; it is not part of gameplay overlay dispatch.

## Deliberately unmodified systems

Chunk construction, chunk uploads, terrain geometry, texture animation, particles,
entities, redstone, block updates, networking, inventory, collision, movement,
combat and server view distance retain the normal 1.12.2 implementation. The
client never changes render distance or quality behind the user's back.

No global GC flags are forced. Default Java 8 ergonomics, G1 and other collectors
must be compared on the actual machine/workload before recommending a collector.
No sleeps, swap interval overrides, forced VSync settings or packet hooks are
added. Physical network latency is unchanged.

## Extending modules

Add immutable metadata to `ModuleDefinition` and provide its data/drawing in
`HudRenderer`. Defaults, validation, profile persistence, the module list,
appearance controls, positioning, hotkeys and editor selection are generic.
New modules must stay client-side and must not change game mechanics.

The benchmark probe is a separate optional instrument, never loaded by the
client JAR. Its one hook observes `Minecraft.runGameLoop`/SRG/1.12.2 obfuscated
equivalents. The same JAR must be used on both sides of a vanilla comparison.
