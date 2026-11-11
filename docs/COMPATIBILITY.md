# Compatibility and limits

- Target: Minecraft Java **1.12.2**, Forge **14.23.5.2860**, 64-bit **Java 8**, LWJGL 2.
  This is a client-only mod package, not a replacement launcher or standalone
  redistribution of Minecraft. A normal licensed Minecraft installation is required.
- No server component is required. Do not put the client JAR on dedicated servers.
  Server rules still determine whether HUD and coordinate mods are permitted.
- No custom channels, packet rewriting, entity logic, movement changes or tick
  skipping are implemented. Ping is the vanilla player-list value. Coordinates
  are information already available through vanilla debug facilities.
- The only game-class transform is `FontRenderer`. OptiFine, alternative font
  renderers, shader loaders and other coremods have **not** been exhaustively
  certified. Start with Forge + Lilt. Disable Text metrics cache when isolating
  font issues. If a transformer signature does not match, Lilt leaves it unchanged.
  The hook validates structure, not the semantics of every third-party patch.
- Resource reloads and vanilla Unicode/bidirectional setters clear the cache.
  A third-party mod that mutates private font tables without those paths must
  invalidate the cache itself or leave this optimization disabled.
- Open Sans covers Latin-1 in the custom atlas; text outside that set falls back
  to Minecraft's renderer. Two fixed font atlases use about 2 MiB of GPU texture
  memory and about 2 MiB of CPU pixel backing, plus font/metadata overhead.
  No downloaded fonts or UI network requests.
- UI is designed for a minimum vanilla scaled display of roughly 320×240. Very
  large modules are fitted to the screen. The Studio scales as one composition;
  very small windows will naturally make type smaller.
- The frame-time graph caps visual bar height at 33.333 ms. Full spikes remain in
  the raw CSV and numerical summaries. The FPS/1% HUD uses only the most recent
  240 intervals; use longer captures for conclusions.
- Capture storage is bounded to one million frame intervals. It stops at capacity
  and records that reason. At 240 FPS this is about 69 minutes. Split longer tests
  into labeled captures. The baseline probe has the same capacity.
- Shutdown flushes are best effort. Abrupt process termination/power loss can lose
  the latest edits or an unfinished capture. An unwritable output directory is
  reported without silently pretending a file was saved.
- No claim of 240 FPS, reduced physical network latency, improved input-to-photon
  latency, or complete compatibility with arbitrary modpacks is made.
- This version does not contain a terrain-renderer replacement, multithreaded
  world access, automatic quality downgrades, custom authentication or updater.
  Further optimization needs workload profiling and visual/gameplay parity tests.

## Legacy development environment

ForgeGradle 3 can inject a merge-tool `Side` enum with an extra BUKKIT constant
into its binary development JAR. That makes Forge's own channel initialization
fail even before mod initialization. The build pins the older annotation artifact
and prepares the **original two annotation classes from the pinned official Forge
universal JAR** at the front of the development launch classpath. These development
support files are not included in the client release. Build-tool-only ASM 6
artifacts are removed from that launch classpath because Forge's ASM 5 scanner
cannot parse their Java 9 module descriptors. Real Forge installations use their
normal official classes.

The upstream issue is documented at
https://github.com/MinecraftForge/ForgeGradle/issues/748 and
https://forums.minecraftforge.net/topic/96644-npe-on-client-start-up/.
