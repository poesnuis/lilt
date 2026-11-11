package dev.lilt.ui;

import dev.lilt.LiltClient;
import dev.lilt.config.*;
import dev.lilt.module.ModuleDefinition;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.io.IOException;

public final class HudEditorScreen extends GuiScreen {
    private final GuiScreen parent;
    private final LiltClient client = LiltClient.instance;
    private ModuleDefinition selected;
    private boolean dragging, resizing;
    private float offsetX, offsetY, initialMouseX;
    private double initialScale;
    public HudEditorScreen(GuiScreen parent) { this.parent = parent; }
    @Override public boolean doesGuiPauseGame() { return false; }
    @Override public void initGui() { FontAtlas.prepare(); }
    @Override public void onGuiClosed() { client.changed(); }
    @Override public void drawScreen(int mx, int my, float partialTicks) {
        if (mc.world == null) drawDefaultBackground();
        Canvas.begin();
        try {
            Canvas.rect(0, 0, width, height, mc.world == null ? 0xffe5eded : 0x28203744);
            Canvas.grid(width, height, 0x3077bad0);
            client.hud.render(width, height, true);
            if (selected != null && client.config.module(selected).enabled) {
                HudRenderer.Bounds b = client.hud.bounds(selected, width, height);
                Canvas.rect(b.x - 2, b.y - 2, b.width + 4, 1, Canvas.PRIMARY);
                Canvas.rect(b.x - 2, b.y + b.height + 1, b.width + 4, 1, Canvas.PRIMARY);
                Canvas.rect(b.x - 2, b.y - 2, 1, b.height + 4, Canvas.PRIMARY);
                Canvas.rect(b.x + b.width + 1, b.y - 2, 1, b.height + 4, Canvas.PRIMARY);
                Canvas.rounded(b.x + b.width - 3, b.y + b.height - 3, 6, 6, 1, Canvas.PRIMARY);
                if (dragging) {
                    Canvas.rect(width / 2f, 0, .5, height, 0x5067d1f5);
                    Canvas.rect(0, height / 2f, width, .5, 0x5067d1f5);
                }
            }
            float bar = Math.min(width - 16, 380), bx = (width - bar) / 2;
            Canvas.panel(bx, height - 37, bar, 29, 7, 1, 0xf4f5fbfc, 0xffcae2e9);
            Canvas.text("HUD EDITOR", bx + 10, height - 29, 8, Canvas.INK, true);
            Canvas.text("Drag / resize / Shift to snap", bx + 83, height - 29, 8, Canvas.MUTED, false);
            Canvas.rounded(bx + bar - 93, height - 32, 42, 19, 4, 0xffe0edf0);
            Canvas.centered("Style", bx + bar - 93, height - 29, 42, 8, Canvas.INK, false);
            Canvas.rounded(bx + bar - 46, height - 32, 38, 19, 4, Canvas.PRIMARY);
            Canvas.centered("Done", bx + bar - 46, height - 29, 38, 8, Canvas.INK, true);
        } finally { Canvas.end(); }
    }
    @Override protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) return;
        float bar = Math.min(width - 16, 380), bx = (width - bar) / 2;
        if (Canvas.hit(mx, my, bx + bar - 46, height - 32, 38, 19)) { mc.displayGuiScreen(parent); return; }
        if (Canvas.hit(mx, my, bx + bar - 93, height - 32, 42, 19)) { mc.displayGuiScreen(new StudioScreen(this, selected == null ? ModuleDefinition.FPS : selected)); return; }
        if (Canvas.hit(mx, my, bx, height - 37, bar, 29)) return;
        if (selected != null) {
            HudRenderer.Bounds b = client.hud.bounds(selected, width, height);
            if (Canvas.hit(mx, my, b.x + b.width - 5, b.y + b.height - 5, 10, 10)) {
                resizing = true; initialMouseX = mx; initialScale = client.config.module(selected).appearance.scale; return;
            }
        }
        ModuleDefinition[] all = ModuleDefinition.values();
        for (int i = all.length - 1; i >= 0; i--) {
            ModuleDefinition definition = all[i];
            if (!definition.hud || !client.config.module(definition).enabled) continue;
            HudRenderer.Bounds b = client.hud.bounds(definition, width, height);
            if (b.contains(mx, my)) { selected = definition; dragging = true; offsetX = mx - b.x; offsetY = my - b.y; return; }
        }
        selected = null;
    }
    @Override protected void mouseClickMove(int mx, int my, int button, long elapsed) {
        if (selected == null) return;
        ModuleConfig module = client.config.module(selected);
        HudRenderer.Bounds b = client.hud.bounds(selected, width, height);
        if (resizing) module.appearance.scale = Config.clamp(initialScale + (mx - initialMouseX) / b.baseWidth, .5, 3, 1);
        else if (dragging) {
            float x = mx - offsetX, y = my - offsetY;
            if (isShiftKeyDown()) { x = Math.round(x / 4) * 4; y = Math.round(y / 4) * 4; }
            module.x = Config.clamp(x / Math.max(1, width - b.width), 0, 1, 0);
            module.y = Config.clamp(y / Math.max(1, height - b.height), 0, 1, 0);
        }
    }
    @Override protected void mouseReleased(int mx, int my, int button) { dragging = resizing = false; client.changed(); }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (selected != null && wheel != 0) {
            Appearance a = client.config.module(selected).appearance;
            a.scale = Config.clamp(a.scale + Math.signum(wheel) * .05, .5, 3, 1); client.changed();
        }
    }
    @Override protected void keyTyped(char ch, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RETURN) { mc.displayGuiScreen(parent); return; }
        if (key == Keyboard.KEY_TAB) {
            int start = selected == null ? -1 : selected.ordinal();
            for (int i = 1; i <= ModuleDefinition.values().length; i++) {
                ModuleDefinition d = ModuleDefinition.values()[(start + i) % ModuleDefinition.values().length];
                if (d.hud && client.config.module(d).enabled) { selected = d; break; }
            }
            return;
        }
        if (selected == null) return;
        ModuleConfig module = client.config.module(selected);
        HudRenderer.Bounds b = client.hud.bounds(selected, width, height);
        int step = isShiftKeyDown() ? 10 : 1;
        if (key == Keyboard.KEY_LEFT) module.x -= step / Math.max(1.0, width - b.width);
        if (key == Keyboard.KEY_RIGHT) module.x += step / Math.max(1.0, width - b.width);
        if (key == Keyboard.KEY_UP) module.y -= step / Math.max(1.0, height - b.height);
        if (key == Keyboard.KEY_DOWN) module.y += step / Math.max(1.0, height - b.height);
        if (key == Keyboard.KEY_DELETE) module.enabled = false;
        module.validate(); client.changed();
    }
}
