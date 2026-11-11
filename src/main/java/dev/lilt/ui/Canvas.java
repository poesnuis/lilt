package dev.lilt.ui;

import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import dev.lilt.metrics.FrameMetrics;

public final class Canvas {
    public static final int INK = 0xff203744, MUTED = 0xff71868e, LINE = 0xffdce7e8;
    public static int PRIMARY = 0xff67d1f5, SECONDARY = 0xffb5f8ff;
    public static final int PAPER = 0xfff5f8f7;
    private static final double[] COS = new double[33], SIN = new double[33];
    private static boolean blend, depth, texture, alpha, cull;
    private static int sourceRgb, targetRgb, sourceAlpha, targetAlpha, boundTexture;
    // LWJGL 2 requires room for sixteen floats even when GL_CURRENT_COLOR returns four.
    private static final FloatBuffer previousColor = BufferUtils.createFloatBuffer(16);
    static { for (int i = 0; i <= 32; i++) { double angle = Math.PI + i * Math.PI / 16; COS[i] = Math.cos(angle); SIN[i] = Math.sin(angle); } }
    private Canvas() { }
    /** A non-nesting scope around the complete overlay or screen; no heap allocation. */
    public static void begin() {
        blend = GL11.glIsEnabled(GL11.GL_BLEND); depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D); alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST); cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB); targetRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA); targetAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        previousColor.clear(); GL11.glGetFloat(GL11.GL_CURRENT_COLOR, previousColor);
        GlStateManager.disableDepth(); GlStateManager.disableCull(); GlStateManager.disableAlpha();
        GlStateManager.enableBlend(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1, 1, 1, 1);
    }
    public static void end() {
        GlStateManager.bindTexture(boundTexture);
        GlStateManager.tryBlendFuncSeparate(sourceRgb, targetRgb, sourceAlpha, targetAlpha);
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
        GlStateManager.color(previousColor.get(0), previousColor.get(1), previousColor.get(2), previousColor.get(3));
    }
    public static int opacity(int color, double opacity) { return (color & 0xffffff) | ((int) ((color >>> 24) * opacity) << 24); }
    public static void text(String text, float x, float y, float size, int color, boolean bold) { FontAtlas.draw(text, x, y, size, color, bold, "Open Sans"); }
    public static void centered(String text, float x, float y, float width, float size, int color, boolean bold) {
        text(text, x + (width - FontAtlas.width(text, size, bold, "Open Sans")) / 2, y, size, color, bold);
    }
    public static boolean hit(double mx, double my, double x, double y, double w, double h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    public static void rect(double x, double y, double width, double height, int color) {
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) return;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, x, y + height, color); vertex(buffer, x + width, y + height, color);
        vertex(buffer, x + width, y, color); vertex(buffer, x, y, color);
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
    }
    public static void rounded(double x, double y, double width, double height, double radius, int color) {
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) return;
        radius = Math.min(Math.max(0, radius), Math.min(width, height) / 2);
        if (radius < .1) { rect(x, y, width, height, color); return; }
        GlStateManager.disableTexture2D(); GlStateManager.enableBlend();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, x + width / 2, y + height / 2, color);
        for (int corner = 0; corner < 4; corner++) {
            double cx = (corner == 0 || corner == 3) ? x + radius : x + width - radius;
            double cy = corner < 2 ? y + radius : y + height - radius;
            for (int step = 0; step <= 8; step++) { int i = corner * 8 + step; vertex(buffer, cx + COS[i] * radius, cy + SIN[i] * radius, color); }
        }
        vertex(buffer, x, y + radius, color);
        Tessellator.getInstance().draw(); GlStateManager.enableTexture2D();
    }
    public static void panel(double x, double y, double width, double height, double radius, double border, int fill, int stroke) {
        if (border > 0) { outline(x, y, width, height, radius, border, stroke); rounded(x + border, y + border, width - 2 * border, height - 2 * border, Math.max(0, radius - border), fill); }
        else rounded(x, y, width, height, radius, fill);
    }
    public static void outline(double x, double y, double width, double height, double radius, double thickness, int color) {
        thickness = Math.min(thickness, Math.min(width, height) / 2);
        radius = Math.min(Math.max(thickness, radius), Math.min(width, height) / 2);
        double innerRadius = Math.max(0, radius - thickness);
        GlStateManager.disableTexture2D(); GlStateManager.enableBlend();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int corner = 0; corner < 4; corner++) {
            double cx = (corner == 0 || corner == 3) ? x + radius : x + width - radius;
            double cy = corner < 2 ? y + radius : y + height - radius;
            for (int step = 0; step <= 8; step++) {
                int i = corner * 8 + step;
                vertex(buffer, cx + COS[i] * radius, cy + SIN[i] * radius, color);
                vertex(buffer, cx + COS[i] * innerRadius, cy + SIN[i] * innerRadius, color);
            }
        }
        vertex(buffer, x, y + radius, color); vertex(buffer, x + thickness, y + radius, color);
        Tessellator.getInstance().draw(); GlStateManager.enableTexture2D();
    }
    public static void graph(FrameMetrics metrics, double x, double y, double width, double height, int color) {
        GlStateManager.disableTexture2D();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int count = Math.min(60, metrics.size());
        for (int i = 0; i < count; i++) {
            double h = Math.min(height, metrics.recentMs(count - 1 - i) / 33.333 * height);
            double left = x + i * width / 60, right = left + Math.max(.5, width / 60 - 1);
            vertex(buffer, left, y + height, color); vertex(buffer, right, y + height, color);
            vertex(buffer, right, y + height - h, color); vertex(buffer, left, y + height - h, color);
        }
        Tessellator.getInstance().draw(); GlStateManager.enableTexture2D();
    }
    public static void grid(int width, int height, int color) {
        GlStateManager.disableTexture2D();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int x = 0; x < width; x += 16) for (int y = 0; y < height; y += 16) {
            vertex(buffer, x, y + .55, color); vertex(buffer, x + .55, y + .55, color);
            vertex(buffer, x + .55, y, color); vertex(buffer, x, y, color);
        }
        Tessellator.getInstance().draw(); GlStateManager.enableTexture2D();
    }
    private static void vertex(BufferBuilder buffer, double x, double y, int color) {
        buffer.pos(x, y, 0).color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24).endVertex();
    }
}
