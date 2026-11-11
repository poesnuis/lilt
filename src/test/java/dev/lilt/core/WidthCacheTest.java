package dev.lilt.core;

import org.junit.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class WidthCacheTest {
    @Before public void reset() { WidthCache.setEnabled(true); WidthCache.invalidate(); }
    @Test public void capacityIsBoundedAndLongStringsBypassCache() {
        Object font = new Object();
        for (int i = 0; i < 20000; i++) WidthCache.remember(i, font, "text " + i);
        assertEquals(WidthCache.CAPACITY, WidthCache.sizeForTests(font));
        assertEquals(-1, WidthCache.find(font, "text 0"));
        String longText = new String(new char[129]);
        WidthCache.remember(500, font, longText);
        assertEquals(-1, WidthCache.find(font, longText));
    }
    @Test public void rendererIdentityAndEpochInvalidate() {
        Object a = new Object(), b = new Object();
        WidthCache.remember(30, a, "abc");
        assertEquals(30, WidthCache.find(a, "abc"));
        assertEquals(-1, WidthCache.find(b, "abc"));
        WidthCache.remember(31, b, "abc");
        WidthCache.invalidate();
        assertEquals(-1, WidthCache.find(b, "abc"));
    }
    @Test public void threadsCannotLeakMetricsIntoOneAnother() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        Object font = new Object();
        try {
            WidthCache.remember(1, font, "same");
            assertEquals(Integer.valueOf(-1), worker.submit(() -> WidthCache.find(font, "same")).get());
            worker.submit(() -> WidthCache.remember(2, font, "same")).get();
            assertEquals(1, WidthCache.find(font, "same"));
            WidthCache.invalidate();
            assertEquals(Integer.valueOf(-1), worker.submit(() -> WidthCache.find(font, "same")).get());
        } finally { worker.shutdownNow(); }
    }
    @Test public void disabledModeDoesNotReadOrPopulate() {
        Object font = new Object();
        WidthCache.remember(5, font, "abc");
        WidthCache.setEnabled(false);
        assertEquals(-1, WidthCache.find(font, "abc"));
        assertEquals(10, WidthCache.remember(10, font, "abc"));
        WidthCache.setEnabled(true);
        assertEquals(-1, WidthCache.find(font, "abc"));
    }
}
