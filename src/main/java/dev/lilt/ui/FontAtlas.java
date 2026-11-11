package dev.lilt.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/** Two small, fixed glyph atlases, uploaded once on the render thread. */
public final class FontAtlas {
    private static final int SIZE = 512, CELL_W = 32, CELL_H = 36, EM = 24;
    private static FontAtlas regular, semibold;
    private final int[] advances = new int[256];
    private final ResourceLocation texture;
    private FontAtlas(boolean bold) {
        BufferedImage bitmap = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bitmap.createGraphics();
        try (InputStream stream = FontAtlas.class.getResourceAsStream("/assets/lilt/fonts/OpenSans-" + (bold ? "Semibold" : "Regular") + ".ttf")) {
            Font font = stream == null ? new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, EM) : Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont((float) EM);
            graphics.setFont(font);
            graphics.setColor(Color.WHITE);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            FontMetrics metrics = graphics.getFontMetrics();
            for (int c = 32; c < 256; c++) {
                int i = c - 32, x = (i % 16) * CELL_W, y = (i / 16) * CELL_H;
                advances[c] = metrics.charWidth((char) c);
                graphics.drawString(String.valueOf((char) c), x + 2, y + 2 + metrics.getAscent());
            }
        } catch (Exception exception) { throw new IllegalStateException("Unable to load Lilt font", exception); }
        finally { graphics.dispose(); }
        DynamicTexture dynamic = new DynamicTexture(bitmap);
        dynamic.setBlurMipmap(true, false);
        texture = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("lilt-font-" + bold, dynamic);
    }
    public static void prepare() { if (regular == null) { regular = new FontAtlas(false); semibold = new FontAtlas(true); } }
    private static boolean nativeText(String text, String family) {
        if ("Minecraft".equals(family)) return true;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) < 32 || text.charAt(i) > 255) return true;
        return false;
    }
    public static float width(String text, float size, boolean bold, String family) {
        if (nativeText(text, family)) return Minecraft.getMinecraft().fontRenderer.getStringWidth(bold ? "\u00a7l" + text : text) * size / 9f;
        prepare();
        FontAtlas atlas = bold ? semibold : regular;
        float width = 0;
        for (int i = 0; i < text.length(); i++) width += atlas.advances[text.charAt(i)];
        return width * size / EM;
    }
    public static void draw(String text, float x, float y, float size, int color, boolean bold, String family) {
        // Vanilla treats alpha values below four as fully opaque; keep hidden text hidden.
        if (text.isEmpty() || (color >>> 24) < 4) return;
        if (nativeText(text, family)) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y + size * .22f, 0);
            GlStateManager.scale(size / 9f, size / 9f, 1);
            Minecraft.getMinecraft().fontRenderer.drawString(bold ? "\u00a7l" + text : text, 0, 0, color, false);
            GlStateManager.popMatrix();
            return;
        }
        prepare();
        FontAtlas atlas = bold ? semibold : regular;
        Minecraft.getMinecraft().getTextureManager().bindTexture(atlas.texture);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        float scale = size / EM;
        int a = color >>> 24, r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i), index = c - 32;
            float u = (index % 16) * CELL_W / (float) SIZE, v = (index / 16) * CELL_H / (float) SIZE;
            float x0 = x - 2 * scale, y0 = y - 2 * scale;
            float x1 = x0 + CELL_W * scale, y1 = y0 + CELL_H * scale;
            float u1 = u + CELL_W / (float) SIZE, v1 = v + CELL_H / (float) SIZE;
            buffer.pos(x0, y1, 0).tex(u, v1).color(r, g, b, a).endVertex();
            buffer.pos(x1, y1, 0).tex(u1, v1).color(r, g, b, a).endVertex();
            buffer.pos(x1, y0, 0).tex(u1, v).color(r, g, b, a).endVertex();
            buffer.pos(x0, y0, 0).tex(u, v).color(r, g, b, a).endVertex();
            x += atlas.advances[c] * scale;
        }
        Tessellator.getInstance().draw();
        GlStateManager.color(1, 1, 1, 1);
    }
}
