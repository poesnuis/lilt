package dev.lilt.config;

import dev.lilt.module.ModuleDefinition;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class Config {
    public static final int SCHEMA = 1;
    public int schemaVersion = SCHEMA;
    public String theme = "Dusk";
    public String primary = "67D1F5";
    public String secondary = "B5F8FF";
    public boolean showWithDebug = false;
    public Map<String, ModuleConfig> modules = new LinkedHashMap<>();

    public static Config defaults() {
        Config config = new Config();
        config.validate();
        config.layout("Compact");
        return config;
    }
    public void validate() {
        if (schemaVersion != SCHEMA) throw new IllegalArgumentException("Unsupported configuration schema: " + schemaVersion);
        primary = hex(primary, "67D1F5");
        secondary = hex(secondary, "B5F8FF");
        if (theme == null || theme.length() > 32) theme = "Custom";
        Map<String, ModuleConfig> valid = new LinkedHashMap<>();
        for (ModuleDefinition definition : ModuleDefinition.values()) {
            ModuleConfig module = modules == null ? null : modules.get(definition.id);
            if (module == null) {
                module = new ModuleConfig();
                module.enabled = definition == ModuleDefinition.FPS || definition == ModuleDefinition.TEXT_CACHE;
                module.y = .03 + definition.ordinal() * .08;
            }
            module.validate();
            valid.put(definition.id, module);
        }
        modules = valid;
    }
    public ModuleConfig module(ModuleDefinition definition) { return modules.get(definition.id); }
    public void layout(String name) {
        int i = 0;
        for (ModuleDefinition definition : ModuleDefinition.values()) {
            if (!definition.hud) continue;
            ModuleConfig module = module(definition);
            if ("Focus".equals(name)) { module.x = 1; module.y = .04 + i * .10; }
            else if ("Balanced".equals(name)) { module.x = i % 2 == 0 ? .02 : .98; module.y = .03 + (i / 2) * .14; }
            else { module.x = .02; module.y = .03 + i * .10; }
            module.y = Math.min(.95, module.y);
            i++;
        }
    }
    public void applyTheme(String name) {
        theme = name;
        primary = "67D1F5";
        secondary = "B5F8FF";
        for (ModuleConfig module : modules.values()) {
            Appearance a = module.appearance;
            a.accent = primary;
            a.foreground = "Dusk".equals(name) ? "EAF5F9" : "203744";
            a.backgroundColor = "Dusk".equals(name) ? "182B36" : ("Paper".equals(name) ? "FBF9F2" : "EFFBFF");
            a.borderColor = "Dusk".equals(name) ? "385665" : "CEE4E9";
        }
    }
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    public static double clamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
    public static String hex(String text, String fallback) {
        if (text == null) return fallback;
        text = text.replaceFirst("^#", "").toUpperCase(Locale.ROOT);
        return text.matches("[0-9A-F]{6}") ? text : fallback;
    }
}
