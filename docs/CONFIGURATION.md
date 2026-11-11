# Configuration

All paths are relative to the selected Minecraft instance's game directory.

```text
config/lilt/
  active.txt                    # active profile name
  profiles/default.json         # complete configuration
  profiles/default.json.bak     # previous valid save
  profiles/default.invalid-*.json
  import.json                   # explicitly imported from Studio
  exports/<profile>.json
  benchmarks/*-frames.csv
  benchmarks/*-summary.json
  benchmarks/font-check.json
```

Changes are serialized into an immutable snapshot on the game thread, then
debounced for 500 ms on one daemon writer. Disk sync and atomic file replacement
run on that worker. Switching profiles flushes the old profile before reading
the new one. A shutdown hook flushes pending settings. Sudden power loss can lose
the most recent unsaved half-second. Atomic replacement is used where the file
system supports it; otherwise a normal replacement is used.

Missing profiles use defaults. Invalid active profiles are preserved under an
`.invalid-*` name, then a valid backup is tried, then defaults. Failed imports or
profile loads leave the current in-memory configuration active. Errors appear in
the Studio footer. If the config directory is unwritable, the client can still
launch with temporary settings.

## Schema 1

Every exported profile is a JSON object with `schemaVersion: 1`, `theme`,
`primary`, `secondary`, `showWithDebug` and `modules`. Module keys are stable IDs:
`fps`, `frame_time`, `coordinates`, `memory`, `ping`, `session`, `clock`,
`keystrokes`, `brand`, and `text_cache`.

Each module has `enabled`, `x`, `y`, `keybind`, and `appearance`. Positions are
fractions from 0 to 1 of the available screen area **after subtracting the
module's rendered size**. Thus `x: 1` keeps a module against the right edge as its
text changes. The editor clamps and fits large modules to the current screen.

`keybind` is an LWJGL 2 keyboard code, with 0 meaning unbound. Shortcuts only run
while no screen is open. The global Studio/editor/capture binds are ordinary
Minecraft key bindings and are stored by Minecraft in `options.txt`.

| Appearance field | Range or values |
|---|---|
| `scale` | 0.5–3.0 |
| `opacity` | 0.1–1.0; applies to the whole module |
| `padding` | 0–24 logical pixels |
| `radius` | 0–20 logical pixels |
| `borderWidth` | 0–4; zero removes the outline |
| `spacing` | 0–16 logical pixels between lines |
| `fontSize` | 8–24 |
| `font` | `Open Sans` or `Minecraft` |
| `semibold` | Boolean; Minecraft uses vanilla bold formatting |
| `align` | `LEFT`, `CENTER`, `RIGHT` |
| `background` | Boolean |
| `animate` | Boolean; one fade-in when a module becomes enabled |
| `animationSpeed` | 0.25–3.0 |
| `foreground`, `backgroundColor`, `borderColor`, `accent` | Six-digit RGB HEX, optional leading `#` on import |

HEX input accepts upper/lowercase. Colors are normalized to uppercase without
`#`. Alpha is controlled separately by opacity. Numeric values are clamped;
invalid optional values use defaults. Unknown modules are ignored and missing
known modules are filled in. Unsupported schema versions, wrong structural
types, files over 1 MiB, nesting beyond 32 levels and symlink imports are rejected.

Profile names are 1–32 ASCII letters/numbers/dashes/underscores and must start
with a letter or number. Paths cannot be supplied as profile names. Imports
read only `config/lilt/import.json`; they never execute scripts or load code.

## Themes and layouts

Studio → Themes changes the primary and secondary Studio accents and provides
Dusk, Daylight and Paper HUD color presets. Presets replace module colors while
retaining sizes, positions, fonts and enabled states. Individual changes in
Appearance override only that module. Save a custom theme name and use Profiles
→ Save as to retain multiple named setups. Themes are stored **inside profiles**;
there is no separate theme file format. Saving an existing profile name replaces
that profile after preserving its last valid version as a backup.

Compact, Balanced and Focus arrange enabled modules using their rendered bounds.
Compact stacks from the left, Balanced alternates sides, and Focus starts at the
right. Newly enabled modules avoid existing ones where space is available.
Very large or numerous modules may still need a smaller scale or manual placement.
Visibility and style remain as configured. `showWithDebug` defaults to false, suppressing Lilt overlays when
vanilla F3 debug information is visible. F1 hides the entire HUD normally.

Exports contain only client settings. Captures include system/runtime/graphics
metadata and performance samples; no player chat, server addresses, access
tokens, inventory, or player coordinates are captured by the recorder.
