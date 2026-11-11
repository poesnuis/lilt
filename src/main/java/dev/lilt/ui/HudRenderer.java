package dev.lilt.ui;

import dev.lilt.LiltClient;
import dev.lilt.config.*;
import dev.lilt.module.ModuleDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Mouse;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class HudRenderer {
    private final LiltClient client;
    private final String[][] labels = new String[ModuleDefinition.values().length][];
    private final Bounds[] bounds = new Bounds[ModuleDefinition.values().length];
    private final long[] appeared = new long[ModuleDefinition.values().length];
    private final boolean[] wasEnabled = new boolean[ModuleDefinition.values().length];
    private long updated;
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public HudRenderer(LiltClient client) {
        this.client = client;
        for (ModuleDefinition module : ModuleDefinition.values()) { labels[module.ordinal()] = new String[]{module.title}; bounds[module.ordinal()] = new Bounds(); }
    }
    public void update(long now) {
        if (now - updated < 250_000_000L) return;
        updated = now;
        Minecraft mc = Minecraft.getMinecraft();
        labels[ModuleDefinition.FPS.ordinal()] = new String[]{String.format(Locale.ROOT, "%.0f FPS", client.frames.summary().averageFps), String.format(Locale.ROOT, "1%% low  %.0f", client.frames.summary().onePercentLowFps)};
        labels[ModuleDefinition.FRAME_TIME.ordinal()] = new String[]{String.format(Locale.ROOT, "%.2f ms / frame", client.frames.summary().meanMs)};
        labels[ModuleDefinition.COORDINATES.ordinal()] = mc.player == null ? new String[]{"X  128    Y  64    Z  -32", "Overworld  /  North"}
            : new String[]{String.format(Locale.ROOT, "X  %.0f    Y  %.0f    Z  %.0f", Math.floor(mc.player.posX), Math.floor(mc.player.posY), Math.floor(mc.player.posZ)),
                mc.world.provider.getDimensionType().getName() + "  /  " + mc.player.getHorizontalFacing().getName()};
        labels[ModuleDefinition.MEMORY.ordinal()] = new String[]{client.system.heapUsedBytes / 1048576 + " MB heap", "GC  " + client.system.gcMillis + " ms total"};
        NetworkPlayerInfo info = mc.player == null || mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(mc.player.getUniqueID());
        labels[ModuleDefinition.PING.ordinal()] = new String[]{mc.isSingleplayer() ? "Local world" : info == null ? "Not connected" : info.getResponseTime() + " ms ping"};
        long seconds = (now - client.sessionStarted) / 1_000_000_000L;
        labels[ModuleDefinition.SESSION.ordinal()] = new String[]{String.format(Locale.ROOT, "%02d:%02d:%02d session", seconds / 3600, seconds / 60 % 60, seconds % 60)};
        labels[ModuleDefinition.CLOCK.ordinal()] = new String[]{LocalTime.now().format(CLOCK_FORMAT)};
        labels[ModuleDefinition.KEYSTROKES.ordinal()] = new String[]{"Input"};
        labels[ModuleDefinition.BRAND.ordinal()] = new String[]{"lilt  /  1.12.2"};
    }
    public Bounds bounds(ModuleDefinition definition, int screenWidth, int screenHeight) {
        ModuleConfig module = client.config.module(definition);
        Appearance a = module.appearance;
        Bounds box = bounds[definition.ordinal()];
        float textWidth = 0;
        for (String line : labels[definition.ordinal()]) textWidth = Math.max(textWidth, FontAtlas.width(line, a.fontSize, a.semibold, a.font));
        box.baseWidth = Math.max(40, textWidth + a.padding * 2);
        box.baseHeight = labels[definition.ordinal()].length * (a.fontSize * 1.4f + a.spacing) - a.spacing + 2 * a.padding;
        if (definition == ModuleDefinition.FRAME_TIME) { box.baseWidth = Math.max(150, box.baseWidth); box.baseHeight += 36; }
        if (definition == ModuleDefinition.KEYSTROKES) { box.baseWidth = Math.max(148, box.baseWidth); box.baseHeight += 56; }
        box.scale = (float) Math.min(a.scale, Math.min(screenWidth / box.baseWidth, screenHeight / box.baseHeight));
        box.width = box.baseWidth * box.scale; box.height = box.baseHeight * box.scale;
        box.x = (float) (Math.max(0, screenWidth - box.width) * module.x);
        box.y = (float) (Math.max(0, screenHeight - box.height) * module.y);
        return box;
    }
    public void render(int screenWidth, int screenHeight, boolean editing) {
        for (ModuleDefinition definition : ModuleDefinition.values()) {
            ModuleConfig module = client.config.module(definition);
            int i = definition.ordinal();
            if (module.enabled && !wasEnabled[i]) appeared[i] = System.nanoTime();
            wasEnabled[i] = module.enabled;
            if (!definition.hud || !module.enabled) continue;
            Bounds box = bounds(definition, screenWidth, screenHeight);
            draw(definition, box.x, box.y, box.scale, editing);
        }
    }
    public void preview(ModuleDefinition definition, float x, float y, float width, float height) {
        Bounds box = bounds(definition, 2000, 2000);
        float scale = Math.min(1.5f, Math.min((width - 24) / box.baseWidth, (height - 24) / box.baseHeight));
        draw(definition, x + (width - box.baseWidth * scale) / 2, y + (height - box.baseHeight * scale) / 2, scale, true);
    }
    public void draw(ModuleDefinition definition, float x, float y, float scale, boolean editing) {
        Appearance a = client.config.module(definition).appearance;
        Bounds box = bounds[definition.ordinal()];
        double alpha = a.opacity;
        if (!editing && a.animate) alpha *= Math.min(1, (System.nanoTime() - appeared[definition.ordinal()]) / (220_000_000.0 / a.animationSpeed));
        int foreground = Canvas.opacity(a.color(a.foreground), alpha), accent = Canvas.opacity(a.color(a.accent), alpha);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0); GlStateManager.scale(scale, scale, 1);
        if (a.background) Canvas.panel(0, 0, box.baseWidth, box.baseHeight, a.radius, a.borderWidth,
            Canvas.opacity(a.color(a.backgroundColor), alpha), Canvas.opacity(a.color(a.borderColor), alpha));
        else if (a.borderWidth > 0) {
            Canvas.outline(0, 0, box.baseWidth, box.baseHeight, a.radius, a.borderWidth, Canvas.opacity(a.color(a.borderColor), alpha));
        }
        float lineY = a.padding;
        for (String line : labels[definition.ordinal()]) {
            float width = FontAtlas.width(line, a.fontSize, a.semibold, a.font);
            float lineX = "CENTER".equals(a.align) ? (box.baseWidth - width) / 2 : "RIGHT".equals(a.align) ? box.baseWidth - a.padding - width : a.padding;
            FontAtlas.draw(line, lineX, lineY, a.fontSize, lineY == a.padding ? foreground : Canvas.opacity(foreground, .7), a.semibold, a.font);
            lineY += a.fontSize * 1.4f + a.spacing;
        }
        if (definition == ModuleDefinition.FRAME_TIME) {
            float graphWidth = box.baseWidth - 2 * a.padding;
            Canvas.rect(a.padding, lineY + 26, graphWidth, 1, Canvas.opacity(foreground, .15));
            Canvas.graph(client.frames, a.padding, lineY, graphWidth, 28, accent);
        }
        if (definition == ModuleDefinition.KEYSTROKES) drawKeys(a, lineY, foreground, accent);
        GlStateManager.popMatrix();
    }
    private void drawKeys(Appearance a, float y, int foreground, int accent) {
        Minecraft mc = Minecraft.getMinecraft();
        float size = 24, gap = 4, start = a.padding;
        key("W", mc.gameSettings.keyBindForward.isKeyDown(), start + size + gap, y, size, foreground, accent);
        key("A", mc.gameSettings.keyBindLeft.isKeyDown(), start, y + size + gap, size, foreground, accent);
        key("S", mc.gameSettings.keyBindBack.isKeyDown(), start + size + gap, y + size + gap, size, foreground, accent);
        key("D", mc.gameSettings.keyBindRight.isKeyDown(), start + 2 * (size + gap), y + size + gap, size, foreground, accent);
        key("L", Mouse.isButtonDown(0), start + 3 * (size + gap) + 8, y, size, foreground, accent);
        key("R", Mouse.isButtonDown(1), start + 3 * (size + gap) + 8, y + size + gap, size, foreground, accent);
    }
    private void key(String label, boolean down, float x, float y, float size, int fg, int accent) {
        Canvas.rounded(x, y, size, size, 3, down ? accent : Canvas.opacity(fg, .12));
        Canvas.centered(label, x, y + 5, size, 10, down ? Canvas.INK : fg, true);
    }
    public void arrange(String layout, int width, int height) {
        boolean[] occupied = new boolean[ModuleDefinition.values().length];
        int index = 0;
        for (ModuleDefinition definition : ModuleDefinition.values()) {
            if (!definition.hud || !client.config.module(definition).enabled) continue;
            boolean right = "Focus".equals(layout) || ("Balanced".equals(layout) && index % 2 == 1);
            if (!place(definition, width, height, occupied, right)) client.notice = "Layout needs more room; reduce scale or hide a module.";
            occupied[definition.ordinal()] = true; index++;
        }
    }
    public void avoidOverlap(ModuleDefinition definition, int width, int height) {
        boolean[] occupied = new boolean[ModuleDefinition.values().length];
        for (ModuleDefinition other : ModuleDefinition.values()) occupied[other.ordinal()] = other != definition && other.hud && client.config.module(other).enabled;
        Bounds box = bounds(definition, width, height);
        if (overlaps(box.x, box.y, box.width, box.height, occupied, width, height)) place(definition, width, height, occupied, false);
    }
    private boolean place(ModuleDefinition definition, int width, int height, boolean[] occupied, boolean right) {
        Bounds box = bounds(definition, width, height);
        float w = box.width, h = box.height;
        float maxX = Math.max(0, width - w - 4), maxY = Math.max(0, height - h - 38);
        for (float offset = 0; offset <= maxX; offset += 4) {
            float x = right ? maxX - offset : Math.min(maxX, 4 + offset);
            for (float y = Math.min(4, maxY); y <= maxY; y += 4) {
                if (overlaps(x, y, w, h, occupied, width, height)) continue;
                ModuleConfig module = client.config.module(definition);
                module.x = x / Math.max(1, width - w); module.y = y / Math.max(1, height - h);
                return true;
            }
        }
        return false;
    }
    private boolean overlaps(float x, float y, float w, float h, boolean[] occupied, int width, int height) {
        for (ModuleDefinition other : ModuleDefinition.values()) {
            if (!occupied[other.ordinal()]) continue;
            Bounds b = bounds(other, width, height);
            if (x < b.x + b.width + 4 && x + w + 4 > b.x && y < b.y + b.height + 4 && y + h + 4 > b.y) return true;
        }
        return false;
    }
    public static final class Bounds {
        public float x, y, width, height, scale, baseWidth, baseHeight;
        public boolean contains(float mx, float my) { return Canvas.hit(mx, my, x, y, width, height); }
    }
}
