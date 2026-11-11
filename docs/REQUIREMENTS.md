# Project: High-Performance Minecraft 1.12.2 Client

You are an experienced Minecraft client developer, Java engineer, and performance optimization specialist. Your task is to design and develop a polished, lightweight, high-performance Minecraft client for **Minecraft Java Edition 1.12.2**.

The goal is to create a client that competes with the performance, usability, and visual polish of clients such as Lunar Client and Badlion Client, while having its own original identity, interface, and architecture.

This is **not a hacked client**. Do not include cheats, combat advantages, exploit modules, movement modifications, or other functionality intended to give players illegitimate advantages. The client should feel like a legitimate performance and customization client.

## 1. Primary objectives

Performance is the highest priority.

The client should aim to:

- Maximize FPS while minimizing CPU, GPU, and memory overhead.
- Reduce input latency and unnecessary frame delays.
- Minimize stuttering, frame-time spikes, and garbage collection interruptions.
- Maintain consistent performance during gameplay, not simply achieve high FPS in an empty world.
- Preserve vanilla Minecraft mechanics and gameplay behavior.
- Remain stable during extended play sessions.

**Target performance:** Aim for 240+ FPS at:

- 16-chunk render distance.
- Maximum available graphics settings.
- Vanilla gameplay mechanics and normal rendering behavior.
- Typical multiplayer and singleplayer scenarios.

Treat 240+ FPS as a performance target, not a guaranteed result on every computer. Optimize aggressively, measure actual results, and document hardware-dependent limitations.

Do not achieve higher FPS by secretly reducing render distance, lowering graphics quality, reducing entity or particle updates, skipping ticks, changing game mechanics, or hiding gameplay-relevant information.

## 2. Performance optimization

Investigate and implement optimizations specifically compatible with Minecraft 1.12.2.

Prioritize:

- Rendering pipeline optimization.
- Chunk rendering and rebuild efficiency.
- Reduced unnecessary draw calls and rendering overhead.
- Efficient entity and block rendering.
- Memory allocation reduction.
- Garbage collection optimization.
- More efficient resource management.
- CPU workload reduction.
- Background task management.
- Improved frame pacing.
- Reduced unnecessary event and callback overhead.
- Efficient texture and resource handling.

Avoid expensive operations on the main game thread whenever possible. Use multithreading only where it is safe and compatible with Minecraft's rendering and game-thread requirements.

Do not introduce race conditions, thread-safety problems, rendering corruption, or inconsistent game state.

### Latency

Minimize avoidable client-side latency, including:

- Input processing delays.
- Frame presentation delays.
- Unnecessary buffering.
- Excessive client-side processing.
- Network-thread bottlenecks.

Do not claim to reduce the player's physical distance to a server or bypass network limitations. Do not manipulate packets, interfere with server behavior, or introduce features that undermine anti-cheat systems.

## 3. Preserve vanilla mechanics

This requirement is critical.

The client must preserve the original gameplay behavior of Minecraft 1.12.2.

Do not:

- Alter player movement, acceleration, or collision.
- Modify attack reach, attack speed, or hit registration.
- Change entity behavior or AI.
- Skip or accelerate game ticks.
- Modify block updates or redstone behavior.
- Change inventory interactions.
- Alter physics or projectile behavior.
- Reduce the effective distance at which gameplay-relevant entities or blocks function.
- Introduce hidden gameplay advantages.

Performance optimizations must not change the underlying game mechanics.

Minecraft 1.12.2 does not have the modern vanilla simulation-distance setting. Preserve normal 1.12.2 ticking and server view-distance behavior rather than attempting to imitate a newer version's setting.

## 4. Customizable GUI and visual identity

The client must have a distinctive, professionally designed GUI.

**Do not simply reuse a standard open-source hacked-client GUI or copy the typical ClickGUI design found in clients such as LiquidBounce, Impact, or other similar clients.**

Create original interface components wherever practical.

The design should feel like a legitimate commercial performance client: clean, modern, cohesive, intuitive, and lightweight.

### Default color palette

Use these as the client's primary accent colors:

- Primary: `#67D1F5`
- Secondary: `#B5F8FF`

These should be the default theme colors, not mandatory colors throughout every element. Use neutral backgrounds and appropriate contrast to keep the interface readable.

### GUI requirements

The GUI must support:

- Draggable interface modules.
- Customizable module positioning.
- Customizable module size where appropriate.
- Adjustable opacity and transparency.
- Multiple interface layouts.
- Customizable module backgrounds.
- Customizable borders, corners, and outlines.
- Adjustable spacing and padding.
- Customizable animations.
- Adjustable animation speed.
- Customizable font selection.
- Font size and weight adjustments.
- Customizable text alignment.
- A color editor with HEX color input.
- Preset themes and the ability to create custom themes.
- Saving and loading GUI configurations.
- Persistent settings between launches.

Users should be able to customize the appearance of individual modules without needing to customize every module globally.

### Design direction

Explore an original visual language rather than defaulting to the familiar dark rectangle-and-colored-text interface.

Possible design characteristics include:

- Minimalist panels.
- Carefully balanced spacing.
- Subtle animations.
- Smooth transitions.
- Refined typography.
- Lightweight visual effects.
- Cohesive iconography.
- Thoughtful hierarchy and layout.

The interface should look intentional and professionally designed. Avoid unnecessary gradients, excessive glow, oversized borders, visual clutter, and generic sci-fi styling.

The GUI should also remain lightweight. Interface animations and visual effects must not meaningfully compromise performance.

## 5. Modular system

Develop a clean, extensible module architecture.

Each module should have:

- A unique name.
- A description.
- An enable/disable state.
- Configurable settings where applicable.
- A customizable appearance.
- An optional keybind.
- A clearly defined purpose.

Modules should be organized into intuitive categories.

Suggested categories:

- Performance.
- Visual.
- Interface.
- Utility.
- Information.

Only implement legitimate client-side functionality. Do not include combat, movement, exploit, or unfair-advantage modules.

Modules must be independently configurable, and adding new modules should not require rewriting the entire GUI or settings system.

## 6. Settings and configuration

Create a robust settings system that supports:

- Persistent configuration files.
- Automatic saving.
- Multiple configuration profiles.
- Importing and exporting configurations.
- Per-module settings.
- GUI customization settings.
- Theme settings.
- Keybind management.
- Safe default values.
- Configuration validation and error handling.

Corrupted or missing configuration files should not prevent the client from launching.

## 7. Compatibility and technical requirements

The target is specifically **Minecraft Java Edition 1.12.2**.

Respect the technical limitations of this version:

- Use a compatible Java runtime and language level.
- Account for the older Minecraft rendering architecture and LWJGL 2.
- Do not assume modern Minecraft APIs are available.
- Do not blindly port optimizations from modern Minecraft versions.
- Preserve compatibility with the target version's resource and rendering systems.

Prefer a maintainable architecture over excessive patches to vanilla code.

Before implementing optimizations, identify the relevant bottlenecks and explain why each optimization is appropriate for 1.12.2.

Do not introduce unnecessary dependencies. Any bundled libraries or fonts must have appropriate licenses.

## 8. Benchmarking and performance verification

Performance claims must be supported by testing.

Develop a benchmarking process that evaluates:

- Average FPS.
- 1% low FPS.
- Frame-time consistency.
- CPU utilization.
- GPU utilization.
- Memory consumption.
- Garbage collection frequency and pauses.
- Input responsiveness where measurable.

Test scenarios should include:

1. An empty world.
2. A normal survival world.
3. A world with substantial terrain and chunk complexity.
4. A multiplayer environment.
5. A scene containing many entities.
6. Extended gameplay sessions.

Compare the performance against an appropriately configured vanilla 1.12.2 installation.

Do not fabricate benchmarks or claim improvements that have not been measured.

Provide clear documentation describing the hardware used, the settings enabled, and the test conditions.

## 9. Deliverables

Produce a complete, functioning project rather than a visual mockup or an unfinished collection of code snippets.

Deliver:

- Complete source code.
- A working build configuration.
- A compiled client or installation package.
- A clean project structure.
- Setup and installation instructions.
- Configuration documentation.
- A list of implemented features.
- Benchmarking results.
- Known limitations and compatibility notes.

The project should compile reliably, launch correctly, and be usable without requiring the user to manually fix unfinished code.

## 10. Development philosophy

Prioritize the following in order:

1. Preserve vanilla gameplay and mechanics.
2. Maximize measurable performance.
3. Maintain stability and compatibility.
4. Minimize client-side latency and stuttering.
5. Build an original, polished, customizable GUI.
6. Maintain clean and extensible code.

Do not sacrifice stability or gameplay integrity just to produce an impressive FPS number.

Do not implement features merely to inflate the feature list. Every feature should have a clear purpose.

The final result should feel like a carefully engineered, legitimate Minecraft performance client: exceptionally fast, lightweight, stable, customizable, and visually distinct.

**The defining characteristic of this client should be that it makes Minecraft 1.12.2 run as efficiently as possible without making it feel like a different game.**
