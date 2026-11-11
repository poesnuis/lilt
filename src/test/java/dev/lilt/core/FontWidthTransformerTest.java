package dev.lilt.core;

import org.junit.*;
import java.io.*;
import java.lang.reflect.*;
import net.minecraft.client.resources.IResourceManager;
import static org.junit.Assert.*;

public class FontWidthTransformerTest {
    @Before public void reset() { WidthCache.setEnabled(true); WidthCache.invalidate(); }
    @Test public void transformedClassVerifiesAndInvalidatesAllMutableInputs() throws Exception {
        byte[] original;
        try (InputStream in = Fixture.class.getResourceAsStream("FontWidthTransformerTest$Fixture.class")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] block = new byte[1024]; int count;
            while ((count = in.read(block)) != -1) out.write(block, 0, count);
            original = out.toByteArray();
        }
        byte[] transformed = new FontWidthTransformer().transform(Fixture.class.getName(), "net.minecraft.client.gui.FontRenderer", original);
        assertFalse(java.util.Arrays.equals(original, transformed));
        Class<?> cls = new ClassLoader(getClass().getClassLoader()) { Class<?> loadPatched() { return defineClass(Fixture.class.getName(), transformed, 0, transformed.length); } }.loadPatched();
        Object font = cls.newInstance();
        Method width = cls.getMethod("getStringWidth", String.class);
        Field calls = cls.getField("calls");
        assertEquals(15, width.invoke(font, "abc"));
        assertEquals(15, width.invoke(font, "abc"));
        assertEquals(1, calls.get(font));
        cls.getMethod("setUnicodeFlag", boolean.class).invoke(font, true);
        assertEquals(18, width.invoke(font, "abc"));
        assertEquals(2, calls.get(font));
        cls.getMethod("setBidiFlag", boolean.class).invoke(font, true);
        assertEquals(19, width.invoke(font, "abc"));
        cls.getMethod("onResourceManagerReload", IResourceManager.class).invoke(font, new Object[]{null});
        assertEquals(22, width.invoke(font, "abc"));
        assertEquals(4, calls.get(font));
        assertEquals(0, width.invoke(font, new Object[]{null}));
        WidthCache.setEnabled(false);
        width.invoke(font, "abc"); width.invoke(font, "abc");
        assertEquals(7, calls.get(font));
    }
    @Test public void unrelatedClassesRemainByteIdentical() {
        byte[] input = {1, 2, 3};
        assertSame(input, new FontWidthTransformer().transform("Other", "Other", input));
    }
    public static class Fixture {
        public int calls, advance = 5;
        public boolean bidi;
        public int getStringWidth(String text) {
            calls++;
            if (text == null) return 0;
            int width = 0;
            for (int i = 0; i < text.length(); i++) width += advance;
            return width + (bidi ? 1 : 0);
        }
        public void setUnicodeFlag(boolean unicode) { advance = unicode ? 6 : 5; }
        public void setBidiFlag(boolean bidi) { this.bidi = bidi; }
        public void onResourceManagerReload(IResourceManager manager) { advance++; }
    }
}
