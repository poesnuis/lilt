package dev.lilt.core;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded, per-thread, per-renderer cache. No Minecraft class may be loaded here. */
public final class WidthCache {
    public static final int CAPACITY = 512;
    private static final AtomicLong EPOCH = new AtomicLong();
    private static volatile boolean enabled = true;
    private static volatile boolean installed;
    private static final ThreadLocal<Local> CACHE = new ThreadLocal<Local>() {
        protected Local initialValue() { return new Local(); }
    };
    private WidthCache() { }
    public static void installed() { installed = true; }
    public static boolean isInstalled() { return installed; }
    public static void setEnabled(boolean value) {
        if (enabled != value) { enabled = value; invalidate(); }
    }
    public static void invalidate() { EPOCH.incrementAndGet(); }
    public static int find(Object renderer, String text) {
        if (!enabled || text == null || text.length() > 128) return -1;
        Local local = local(renderer);
        Integer value = local.values.get(text);
        return value == null ? -1 : value;
    }
    public static int remember(int width, Object renderer, String text) {
        if (enabled && width >= 0 && text != null && text.length() <= 128) {
            local(renderer).values.put(text, width);
        }
        return width;
    }
    private static Local local(Object renderer) {
        Local local = CACHE.get();
        long epoch = EPOCH.get();
        if (local.renderer.get() != renderer || local.epoch != epoch) {
            local.values.clear();
            local.renderer = new WeakReference<>(renderer);
            local.epoch = epoch;
        }
        return local;
    }
    static int sizeForTests(Object renderer) { return local(renderer).values.size(); }
    private static final class Local {
        WeakReference<Object> renderer = new WeakReference<>(null);
        long epoch = -1;
        final Map<String, Integer> values = new LinkedHashMap<String, Integer>(CAPACITY, .75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, Integer> entry) { return size() > CAPACITY; }
        };
    }
}
