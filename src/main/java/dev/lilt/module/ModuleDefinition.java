package dev.lilt.module;

/** Immutable metadata; adding a HUD module does not require changing the Studio. */
public enum ModuleDefinition {
    FPS("fps", "Frame rate", "Average FPS and 1% low over the latest 240 frame intervals.", "Information", true),
    FRAME_TIME("frame_time", "Frame time", "A rolling graph of CPU-observed frame intervals.", "Performance", true),
    COORDINATES("coordinates", "Coordinates", "Your vanilla position, dimension and facing.", "Information", true),
    MEMORY("memory", "Memory", "Used Java heap and recent garbage collection time.", "Performance", true),
    PING("ping", "Connection", "Latency reported in the vanilla player list. No extra packets.", "Information", true),
    SESSION("session", "Session", "Elapsed time since this client was started.", "Utility", true),
    CLOCK("clock", "Local time", "The computer's local clock, refreshed once per second.", "Utility", true),
    KEYSTROKES("keystrokes", "Input display", "Shows held movement keys. Never changes player input.", "Interface", true),
    BRAND("brand", "Signature", "A quiet Lilt signature for your HUD.", "Visual", true),
    TEXT_CACHE("text_cache", "Text metrics cache", "Reuses repeated vanilla string widths; 512 entries per thread.", "Performance", false);

    public final String id, title, description, category;
    public final boolean hud;
    ModuleDefinition(String id, String title, String description, String category, boolean hud) {
        this.id = id; this.title = title; this.description = description; this.category = category; this.hud = hud;
    }
    public static ModuleDefinition byId(String id) {
        for (ModuleDefinition module : values()) if (module.id.equals(id)) return module;
        return null;
    }
}
