package dev.lilt.config;

public final class ModuleConfig {
    public boolean enabled;
    /** Normalized position in the available (screen minus module) area. */
    public double x = .02, y = .03;
    public int keybind = 0;
    public Appearance appearance = new Appearance();
    public void validate() {
        x = Config.clamp(x, 0, 1, .02);
        y = Config.clamp(y, 0, 1, .03);
        keybind = Config.clamp(keybind, 0, 255);
        if (appearance == null) appearance = new Appearance();
        appearance.validate();
    }
}
