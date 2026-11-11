package dev.lilt.ui;

import dev.lilt.LiltClient;
import dev.lilt.config.*;
import dev.lilt.core.WidthCache;
import dev.lilt.module.ModuleDefinition;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/** A single, purpose-built Studio. No floating ClickGUI windows or reflection-based settings. */
public final class StudioScreen extends GuiScreen {
    private static final String[] TABS = {"Overview", "Modules", "Appearance", "Profiles", "Measurements", "Themes"};
    private static final String[] SCENARIOS = {"Empty world", "Survival", "Terrain", "Multiplayer", "Entities", "Extended"};
    private static final String[] CATEGORIES = {"All modules", "Performance", "Visual", "Interface", "Utility", "Information"};
    private static final int[] DURATIONS = {30, 60, 180, 1800, 3600};
    private final GuiScreen parent;
    private final LiltClient client = LiltClient.instance;
    private final List<Control> controls = new ArrayList<>();
    private final Map<String, Entry> entries = new HashMap<>();
    private List<String> profiles = Collections.emptyList();
    private Future<List<String>> profileList;
    private long lastList;
    private int tab, category, scenario = 1, duration = 1, profilePage;
    private ModuleDefinition selected = ModuleDefinition.FPS;
    private String selectedProfile, focused;
    private boolean binding, selectAll;
    private Control dragging;
    private float scale, originX, originY, mouseX, mouseY;

    public StudioScreen(GuiScreen parent) { this.parent = parent; }
    public StudioScreen(GuiScreen parent, ModuleDefinition selected) { this.parent = parent; this.selected = selected; tab = 2; }
    @Override public void initGui() {
        Keyboard.enableRepeatEvents(true);
        FontAtlas.prepare();
        selectedProfile = client.activeProfile;
    }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); client.changed(); }
    @Override public boolean doesGuiPauseGame() { return false; }
    @Override public void updateScreen() {
        if (tab == 3 && client.store != null) {
            if (profileList != null && profileList.isDone()) {
                try { profiles = profileList.get(); } catch (Exception failure) { client.notice = "Could not list profiles."; }
                profileList = null;
            }
            if (profileList == null && System.nanoTime() - lastList > 1_000_000_000L) {
                lastList = System.nanoTime(); profileList = client.store.listAsync();
            }
        }
    }
    @Override public void drawScreen(int mx, int my, float partialTicks) {
        scale = Math.min(width / 900f, height / 540f);
        originX = (width - 900 * scale) / 2; originY = (height - 540 * scale) / 2;
        mouseX = (mx - originX) / scale; mouseY = (my - originY) / scale;
        controls.clear();
        Canvas.PRIMARY = 0xff000000 | Integer.parseInt(client.config.primary, 16);
        Canvas.SECONDARY = 0xff000000 | Integer.parseInt(client.config.secondary, 16);
        Canvas.begin();
        try {
            Canvas.rect(0, 0, width, height, 0xeeedf3f3);
            GlStateManager.pushMatrix();
            GlStateManager.translate(originX, originY, 0); GlStateManager.scale(scale, scale, 1);
            Canvas.rect(0, 0, 900, 540, Canvas.PAPER);
            Canvas.rect(0, 0, 180, 540, 0xffeaf0ef);
            Canvas.rect(179, 0, 1, 540, Canvas.LINE);
            logo(24, 26);
            Canvas.text("YOUR GAME, CONSIDERED.", 24, 81, 8, Canvas.MUTED, true);
            for (int i = 0; i < TABS.length; i++) {
                final int next = i;
                float y = 130 + i * 48;
                boolean hover = Canvas.hit(mouseX, mouseY, 14, y, 152, 38);
                if (tab == i || hover) Canvas.rounded(14, y, 152, 38, 7, tab == i ? Canvas.SECONDARY : 0xffdfe9e9);
                Canvas.text("0" + (i + 1), 25, y + 11, 10, tab == i ? 0xff28708b : Canvas.MUTED, true);
                Canvas.text(TABS[i], 49, y + 9, 12, Canvas.INK, tab == i);
                control(14, y, 152, 38, () -> { tab = next; focused = null; binding = false; });
            }
            button("Arrange HUD", 20, 440, 140, 34, true, () -> mc.displayGuiScreen(new HudEditorScreen(this)));
            Canvas.text("MINECRAFT 1.12.2", 24, 494, 9, Canvas.MUTED, true);
            Canvas.text("Lilt 1.0.0  /  Forge", 24, 510, 9, Canvas.MUTED, false);
            button("×", 848, 20, 30, 28, false, () -> mc.displayGuiScreen(parent));
            if (tab == 0) overview();
            else if (tab == 1) modules();
            else if (tab == 2) appearance();
            else if (tab == 3) profiles();
            else if (tab == 4) measurements();
            else themes();
            Canvas.rect(204, 505, 674, 1, Canvas.LINE);
            String message = client.notice.isEmpty() ? client.store == null ? "Settings are temporary" : client.store.message() : client.notice;
            Canvas.rounded(212, 520, 4, 4, 2, Canvas.PRIMARY);
            Canvas.text(ellipsize(message, 642, 9), 225, 514, 9, Canvas.MUTED, false);
            GlStateManager.popMatrix();
        } finally { Canvas.end(); }
    }
    private void logo(float x, float y) {
        Canvas.rounded(x, y + 5, 29, 35, 7, Canvas.PRIMARY);
        Canvas.rect(x + 9, y + 13, 4, 19, Canvas.INK);
        Canvas.rect(x + 13, y + 28, 8, 4, Canvas.INK);
        Canvas.text("lilt", x + 40, y - 3, 36, Canvas.INK, true);
    }
    private void heading(String eyebrow, String title, String subtitle) {
        Canvas.text(eyebrow, 212, 25, 9, 0xff39839b, true);
        Canvas.text(title, 210, 45, 26, Canvas.INK, true);
        Canvas.text(subtitle, 212, 86, 11, Canvas.MUTED, false);
    }
    private void overview() {
        heading("WELCOME TO LILT", "Make room for the game.", "A considered HUD. Clear measurements. Vanilla at heart.");
        Canvas.panel(212, 122, 656, 138, 12, 1, 0xffe0f6fc, 0xffcce8ef);
        Canvas.text("PLAY YOUR WAY", 233, 141, 9, 0xff39839b, true);
        Canvas.text("Less clutter. More world.", 232, 162, 23, Canvas.INK, true);
        Canvas.text("Start with a few useful details, then make them yours.", 233, 200, 11, 0xff537987, false);
        button("Open HUD editor", 234, 224, 136, 25, true, () -> mc.displayGuiScreen(new HudEditorScreen(this)));
        Canvas.rounded(703, 146, 132, 76, 10, 0xfff6fdff);
        Canvas.text("VANILLA", 722, 157, 11, Canvas.INK, true);
        Canvas.text("mechanics preserved", 718, 181, 9, Canvas.MUTED, false);
        Canvas.rect(719, 207, 96, 2, Canvas.PRIMARY);
        stat("AVERAGE FPS", format("%.0f", client.frames.summary().averageFps), "rolling 240-frame window", 212, 281, 208);
        stat("1% LOW", format("%.0f", client.frames.summary().onePercentLowFps), "slowest 1% of intervals", 436, 281, 208);
        stat("JAVA HEAP", client.system.heapUsedBytes / 1048576 + " MB", "sampled once per second", 660, 281, 208);
        Canvas.text("A small toolkit, thoughtfully arranged.", 212, 402, 16, Canvas.INK, true);
        Canvas.text("Profile  /  " + client.activeProfile, 212, 434, 11, Canvas.MUTED, false);
        Canvas.text("Right Shift  Studio     F9  Arrange HUD     F8  Capture", 212, 465, 10, Canvas.MUTED, false);
        button("Choose modules", 710, 433, 158, 33, false, () -> tab = 1);
    }
    private void stat(String label, String value, String note, float x, float y, float w) {
        Canvas.panel(x, y, w, 98, 9, 1, 0xfffdfefe, Canvas.LINE);
        Canvas.text(label, x + 16, y + 13, 9, Canvas.MUTED, true);
        Canvas.text(value, x + 15, y + 32, 26, Canvas.INK, true);
        Canvas.text(note, x + 16, y + 74, 9, Canvas.MUTED, false);
    }
    private void modules() {
        heading("YOUR TOOLKIT", "Useful by design.", "Enable only the details you want. Every module has its own settings.");
        button(CATEGORIES[category] + "  +", 212, 118, 250, 29, false, () -> category = (category + 1) % CATEGORIES.length);
        float y = 157;
        for (ModuleDefinition definition : ModuleDefinition.values()) {
            if (category != 0 && !definition.category.equals(CATEGORIES[category])) continue;
            final ModuleDefinition target = definition;
            boolean active = selected == definition;
            Canvas.rounded(212, y, 250, 31, 5, active ? 0xffdef4fa : 0xffedf2f2);
            Canvas.text(definition.title, 225, y + 7, 11, Canvas.INK, active);
            Canvas.rounded(440, y + 12, 6, 6, 3, client.config.module(definition).enabled ? 0xff42a5bb : 0xffbdcccc);
            control(212, y, 250, 31, () -> select(target));
            y += 34;
        }
        ModuleConfig module = client.config.module(selected);
        Canvas.text(selected.category.toUpperCase(Locale.ROOT), 493, 122, 9, Canvas.MUTED, true);
        Canvas.text(selected.title, 492, 145, 23, Canvas.INK, true);
        wrap(selected.description, 493, 183, 355, 11, Canvas.MUTED, 17);
        button(module.enabled ? "Enabled  /  click to disable" : "Disabled  /  click to enable", 493, 236, 355, 35, module.enabled, () -> client.toggle(selected));
        if (selected.hud) {
            Canvas.rounded(493, 287, 355, 104, 8, 0xffe5edee);
            client.hud.preview(selected, 493, 287, 355, 104);
            button("Customize appearance", 493, 404, 355, 32, false, () -> { entries.clear(); tab = 2; });
        } else {
            wrap(WidthCache.isInstalled() ? "The font hook is installed. No ticks, game rules, geometry or network packets are changed." : "Font hook unavailable. Vanilla rendering remains active; the toggle has no effect.", 493, 292, 350, 12, Canvas.INK, 21);
        }
        String key = module.keybind == 0 ? "None" : Keyboard.getKeyName(module.keybind);
        button(binding ? "Press a key; Backspace clears; Esc cancels" : "Keybind  /  " + key, 493, 449, 355, 32, false, () -> { binding = true; focused = null; });
    }
    private void appearance() {
        if (!selected.hud) select(ModuleDefinition.FPS);
        heading("MAKE IT YOURS", "Every detail, considered.", "Appearance changes apply only to the selected module. Theme presets apply to all.");
        button("<", 212, 116, 30, 28, false, () -> cycleModule(-1));
        Canvas.text(selected.title, 254, 120, 13, Canvas.INK, true);
        button(">", 575, 116, 30, 28, false, () -> cycleModule(1));
        Appearance a = client.config.module(selected).appearance;
        slider("Scale", 212, 164, 182, .5, 3, a.scale, v -> a.scale = v, false);
        slider("Opacity", 212, 207, 182, .1, 1, a.opacity, v -> a.opacity = v, false);
        slider("Font size", 212, 250, 182, 8, 24, a.fontSize, v -> a.fontSize = (int) v, true);
        slider("Corner radius", 212, 293, 182, 0, 20, a.radius, v -> a.radius = (int) v, true);
        slider("Padding", 212, 336, 182, 0, 24, a.padding, v -> a.padding = (int) v, true);
        slider("Line spacing", 212, 379, 182, 0, 16, a.spacing, v -> a.spacing = (int) v, true);
        slider("Border width", 212, 422, 182, 0, 4, a.borderWidth, v -> a.borderWidth = (int) v, true);
        slider("Animation speed", 212, 465, 182, .25, 3, a.animationSpeed, v -> a.animationSpeed = v, false);
        button("Font  /  " + a.font, 425, 164, 181, 31, false, () -> { a.font = a.font.equals("Open Sans") ? "Minecraft" : "Open Sans"; changed(); });
        button("Weight  /  " + (a.semibold ? "Semibold" : "Regular"), 425, 207, 181, 31, false, () -> { a.semibold = !a.semibold; changed(); });
        button("Align  /  " + a.align, 425, 250, 181, 31, false, () -> { a.align = a.align.equals("LEFT") ? "CENTER" : a.align.equals("CENTER") ? "RIGHT" : "LEFT"; changed(); });
        button("Background  /  " + (a.background ? "On" : "Off"), 425, 293, 181, 31, false, () -> { a.background = !a.background; changed(); });
        button("Fade in  /  " + (a.animate ? "On" : "Off"), 425, 336, 181, 31, false, () -> { a.animate = !a.animate; changed(); });
        Canvas.text("THEME PRESETS", 425, 395, 9, Canvas.MUTED, true);
        button("Dusk", 425, 419, 56, 30, false, () -> theme("Dusk"));
        button("Day", 487, 419, 56, 30, false, () -> theme("Daylight"));
        button("Paper", 549, 419, 57, 30, false, () -> theme("Paper"));
        Canvas.text("Custom colors travel with your profile.", 425, 466, 9, Canvas.MUTED, false);
        Canvas.text("LIVE PREVIEW", 639, 122, 9, Canvas.MUTED, true);
        Canvas.rounded(636, 145, 232, 112, 8, 0xffe2eced);
        client.hud.preview(selected, 636, 145, 232, 112);
        colorEntry("foreground", "TEXT", 636, 278, a.foreground, value -> a.foreground = value);
        colorEntry("background", "BACKGROUND", 636, 329, a.backgroundColor, value -> a.backgroundColor = value);
        colorEntry("border", "BORDER", 636, 380, a.borderColor, value -> a.borderColor = value);
        colorEntry("accent", "ACCENT", 636, 431, a.accent, value -> a.accent = value);
    }
    private void profiles() {
        heading("ROOM FOR EVERY MOOD", "Keep your setup close.", "Profiles save modules, layouts, typography, colors and keybinds together.");
        Canvas.text("SAVED PROFILES", 212, 124, 9, Canvas.MUTED, true);
        int offset = profilePage * 7;
        for (int i = offset; i < Math.min(profiles.size(), offset + 7); i++) {
            final String name = profiles.get(i);
            button(name + (name.equals(client.activeProfile) ? "  / active" : ""), 212, 148 + (i - offset) * 39, 260, 32, name.equals(selectedProfile), () -> selectedProfile = name);
        }
        if (profiles.isEmpty()) Canvas.text("Your first profile is being saved.", 213, 154, 11, Canvas.MUTED, false);
        button("Previous", 212, 432, 123, 29, false, () -> profilePage = Math.max(0, profilePage - 1));
        button("Next", 349, 432, 123, 29, false, () -> profilePage = Math.min(Math.max(0, (profiles.size() - 1) / 7), profilePage + 1));
        button("Load selected", 212, 468, 260, 28, false, () -> { if (selectedProfile != null) client.loadProfile(selectedProfile); });
        Canvas.text("SAVE A SNAPSHOT", 506, 124, 9, Canvas.MUTED, true);
        input("profile-name", "my-setup", 506, 148, 217, 32, 32, value -> { client.createProfile(value); selectedProfile = value; });
        button("Save as", 736, 148, 132, 32, true, () -> { client.createProfile(entries.get("profile-name").value); selectedProfile = client.activeProfile; });
        Canvas.text("Saving as an existing name replaces that profile.", 506, 190, 10, Canvas.MUTED, false);
        Canvas.text("STARTING LAYOUTS", 506, 234, 9, Canvas.MUTED, true);
        button("Compact", 506, 258, 112, 31, false, () -> client.applyLayout("Compact"));
        button("Balanced", 630, 258, 112, 31, false, () -> client.applyLayout("Balanced"));
        button("Focus", 754, 258, 114, 31, false, () -> client.applyLayout("Focus"));
        Canvas.text("Move modules freely in the HUD editor after applying.", 506, 301, 10, Canvas.MUTED, false);
        Canvas.text("TAKE YOUR SETTINGS WITH YOU", 506, 349, 9, Canvas.MUTED, true);
        button("Export active profile", 506, 373, 362, 32, false, () -> {
            if (client.store != null) { client.notice = ""; client.store.exportAsync(client.activeProfile, client.config); }
        });
        button("Import config/lilt/import.json", 506, 415, 362, 32, false, client::importProfile);
        wrap("Exports are saved in config/lilt/exports. Import validates the file and loads it as the imported profile.", 506, 460, 358, 10, Canvas.MUTED, 15);
    }
    private void measurements() {
        heading("MEASURE, THEN IMPROVE", "Know your frame times.", "Capture real frame intervals and system samples. No estimated performance claims.");
        stat("AVERAGE FPS", format("%.1f", client.frames.summary().averageFps), "rolling 240-frame window", 212, 125, 208);
        stat("P99 FRAME TIME", format("%.2f ms", client.frames.summary().p99Ms), "nearest-rank percentile", 436, 125, 208);
        stat("PROCESS CPU", client.system.processCpuPercent < 0 ? "Unavailable" : format("%.1f%%", client.system.processCpuPercent), "share of all logical CPUs", 660, 125, 208);
        Canvas.text("CAPTURE A SCENARIO", 212, 249, 9, Canvas.MUTED, true);
        button(SCENARIOS[scenario] + "  +", 212, 274, 248, 34, false, () -> scenario = (scenario + 1) % SCENARIOS.length);
        button(DURATIONS[duration] + " seconds  +", 476, 274, 168, 34, false, () -> duration = (duration + 1) % DURATIONS.length);
        button(client.recorder.active() ? "Stop capture" : "Start capture", 660, 274, 208, 34, true, () -> client.capture(SCENARIOS[scenario], DURATIONS[duration]));
        Canvas.panel(212, 325, 656, 64, 8, 1, 0xffe8f5f8, 0xffcce5eb);
        Canvas.text(ellipsize(client.recorder.status, 610, 12), 229, 339, 12, Canvas.INK, true);
        Canvas.text(client.recorder.active() ? format("%.0f seconds  /  %,d intervals", client.recorder.elapsedSeconds(System.nanoTime()), client.recorder.frameCount()) : "CSV frames + JSON summary are written to config/lilt/benchmarks.", 229, 365, 10, Canvas.MUTED, false);
        Canvas.text("SET UP A FAIR COMPARISON", 212, 412, 9, Canvas.MUTED, true);
        wrap("Use the same world, route, resolution, 16-chunk distance and graphics settings. Warm up first. Close menus while recording; menu frames are tagged and excluded from the gameplay summary.", 212, 435, 650, 11, Canvas.MUTED, 17);
        Canvas.text("GPU utilization and input-to-photon latency require external tools.", 212, 482, 10, Canvas.MUTED, false);
    }
    private void themes() {
        heading("A PALETTE OF YOUR OWN", "Find your shade.", "Choose HUD colors, tune the Studio accents, and save a theme in your profile.");
        String[] names = {"Dusk", "Daylight", "Paper"};
        int[] fills = {0xff182b36, 0xffeffbff, 0xfffbf9f2};
        int[] inks = {0xffeaf5f9, Canvas.INK, Canvas.INK};
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            float x = 212 + i * 224;
            Canvas.panel(x, 125, 208, 136, 10, 1, fills[i], Canvas.LINE);
            Canvas.text("lilt  /  " + name, x + 20, 143, 15, inks[i], true);
            Canvas.text("Type, space, a little color.", x + 20, 178, 10, Canvas.opacity(inks[i], .7), false);
            Canvas.rounded(x + 20, 207, 28, 9, 3, Canvas.PRIMARY);
            Canvas.rounded(x + 55, 207, 28, 9, 3, Canvas.SECONDARY);
            button("Apply " + name, x, 275, 208, 31, false, () -> theme(name));
        }
        colorEntry("studio-primary", "STUDIO PRIMARY", 212, 337, client.config.primary, value -> client.config.primary = value);
        colorEntry("studio-secondary", "STUDIO SECONDARY", 468, 337, client.config.secondary, value -> client.config.secondary = value);
        button("Use primary on HUD", 724, 353, 144, 28, false, () -> {
            for (ModuleConfig m : client.config.modules.values()) m.appearance.accent = client.config.primary;
            changed();
        });
        Canvas.text("CUSTOM THEME NAME", 212, 410, 9, Canvas.MUTED, true);
        input("theme-name", client.config.theme, 212, 433, 250, 32, 32, value -> {
            if (!value.trim().isEmpty()) { client.config.theme = value.trim(); changed(); }
        });
        button("Save theme to profile", 478, 433, 222, 32, true, () -> {
            String name = entries.get("theme-name").value.trim();
            if (!name.isEmpty()) { client.config.theme = name; changed(); client.notice = "Theme saved in profile " + client.activeProfile; }
        });
        Canvas.text("Use Profiles > Save as to keep multiple custom themes.", 212, 481, 10, Canvas.MUTED, false);
    }
    private void theme(String name) { client.config.applyTheme(name); entries.clear(); changed(); }
    private void select(ModuleDefinition definition) { selected = definition; entries.clear(); focused = null; binding = false; }
    private void cycleModule(int delta) {
        int n = ModuleDefinition.values().length;
        int index = selected.ordinal();
        do { index = (index + delta + n) % n; } while (!ModuleDefinition.values()[index].hud);
        select(ModuleDefinition.values()[index]);
    }
    private void changed() { client.notice = ""; client.changed(); }
    private void button(String label, float x, float y, float w, float h, boolean active, Runnable action) {
        boolean hover = Canvas.hit(mouseX, mouseY, x, y, w, h);
        Canvas.panel(x, y, w, h, 6, active ? 0 : 1, active ? hover ? 0xff89ddf7 : Canvas.PRIMARY : hover ? 0xffe3edef : 0xfffafcfc, active ? Canvas.PRIMARY : Canvas.LINE);
        Canvas.centered(label, x, y + (h - 15) / 2 - 1, w, 11, Canvas.INK, active);
        control(x, y, w, h, action);
    }
    private void control(float x, float y, float w, float h, Runnable action) { controls.add(new Control(x, y, w, h, action)); }
    private void slider(String label, float x, float y, float w, double min, double max, double value, DoubleConsumer apply, boolean integer) {
        Canvas.text(label, x, y, 10, Canvas.INK, false);
        Canvas.text(integer ? Integer.toString((int) value) : format("%.2f", value), x + w - 31, y, 10, Canvas.MUTED, false);
        float ratio = (float) ((value - min) / (max - min));
        Canvas.rounded(x, y + 23, w, 3, 1.5, 0xffd3e1e4);
        Canvas.rounded(x, y + 23, w * ratio, 3, 1.5, 0xff45b8de);
        Canvas.rounded(x + w * ratio - 4, y + 20, 8, 9, 4, 0xff2181a1);
        Control control = new Control(x, y + 15, w, 20, null);
        control.drag = mx -> {
            double result = min + Math.max(0, Math.min(1, (mx - x) / w)) * (max - min);
            apply.accept(integer ? Math.round(result) : Math.round(result * 100) / 100.0);
            changed();
        };
        controls.add(control);
    }
    private void colorEntry(String id, String label, float x, float y, String initial, Consumer<String> apply) {
        Canvas.text(label + "  /  HEX", x, y, 9, Canvas.MUTED, true);
        Consumer<String> commit = value -> {
            if (!value.matches("#?[0-9a-fA-F]{6}")) { client.notice = "Enter a six-digit HEX color, such as #67D1F5."; return; }
            apply.accept(Config.hex(value, initial)); client.config.theme = "Custom"; changed();
        };
        input(id, "#" + initial, x, y + 16, 148, 28, 7, commit);
        Canvas.rounded(x + 154, y + 21, 18, 18, 4, 0xff000000 | Integer.parseInt(initial, 16));
        button("Set", x + 181, y + 16, 51, 28, false, () -> commit.accept(entries.get(id).value));
    }
    private void input(String id, String initial, float x, float y, float w, float h, int max, Consumer<String> commit) {
        Entry entry = entries.computeIfAbsent(id, key -> new Entry(initial, max)); entry.commit = commit;
        boolean active = id.equals(focused);
        Canvas.panel(x, y, w, h, 5, 1, 0xffffffff, active ? 0xff45b8de : Canvas.LINE);
        Canvas.text(entry.value + (active && System.currentTimeMillis() % 1000 < 500 ? "|" : ""), x + 10, y + 5, 11, Canvas.INK, false);
        control(x, y, w, h, () -> { focused = id; selectAll = false; binding = false; });
    }
    @Override protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) return;
        mouseX = (mx - originX) / scale; mouseY = (my - originY) / scale;
        focused = null;
        for (int i = controls.size() - 1; i >= 0; i--) {
            Control control = controls.get(i);
            if (Canvas.hit(mouseX, mouseY, control.x, control.y, control.w, control.h)) {
                if (control.drag != null) { dragging = control; control.drag.accept(mouseX); }
                else control.action.run();
                return;
            }
        }
    }
    @Override protected void mouseClickMove(int mx, int my, int button, long elapsed) { if (dragging != null) dragging.drag.accept((mx - originX) / scale); }
    @Override protected void mouseReleased(int mx, int my, int button) { dragging = null; }
    @Override protected void keyTyped(char ch, int key) throws IOException {
        if (binding) {
            if (key != Keyboard.KEY_ESCAPE) { client.config.module(selected).keybind = key == Keyboard.KEY_BACK ? 0 : key; changed(); }
            binding = false; return;
        }
        Entry entry = focused == null ? null : entries.get(focused);
        if (entry != null) {
            if (key == Keyboard.KEY_ESCAPE) { focused = null; return; }
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { entry.commit.accept(entry.value); focused = null; return; }
            if (isKeyComboCtrlA(key)) { selectAll = true; return; }
            if (isKeyComboCtrlC(key)) { setClipboardString(entry.value); return; }
            if (isKeyComboCtrlV(key)) {
                String value = getClipboardString().replaceAll("[^ -~]", "");
                entry.value = selectAll ? value : entry.value + value;
                entry.value = entry.value.substring(0, Math.min(entry.max, entry.value.length())); selectAll = false; return;
            }
            if (key == Keyboard.KEY_BACK) {
                entry.value = selectAll ? "" : entry.value.substring(0, Math.max(0, entry.value.length() - 1)); selectAll = false; return;
            }
            if (ch >= 32 && ch <= 126) {
                if (selectAll) entry.value = "";
                if (entry.value.length() < entry.max) entry.value += ch;
                selectAll = false;
            }
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }
    private static String format(String format, Object... values) { return String.format(Locale.ROOT, format, values); }
    private static String ellipsize(String text, float maxWidth, float size) {
        if (FontAtlas.width(text, size, false, "Open Sans") <= maxWidth) return text;
        while (text.length() > 1 && FontAtlas.width(text + "...", size, false, "Open Sans") > maxWidth) text = text.substring(0, text.length() - 1);
        return text + "...";
    }
    private static void wrap(String text, float x, float y, float width, float size, int color, float lineHeight) {
        String line = "";
        for (String word : text.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && FontAtlas.width(test, size, false, "Open Sans") > width) { Canvas.text(line, x, y, size, color, false); y += lineHeight; line = word; }
            else line = test;
        }
        Canvas.text(line, x, y, size, color, false);
    }
    private static final class Control {
        final float x, y, w, h;
        final Runnable action;
        DoubleConsumer drag;
        Control(float x, float y, float w, float h, Runnable action) { this.x = x; this.y = y; this.w = w; this.h = h; this.action = action; }
    }
    private static final class Entry {
        String value; final int max; Consumer<String> commit;
        Entry(String value, int max) { this.value = value; this.max = max; }
    }
}
