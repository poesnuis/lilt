package dev.lilt.probe;

import java.io.*;
import java.lang.management.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Read-only observer. It does not alter settings, game ticks or rendering decisions. */
public final class FrameProbe {
    private static final long[] FRAMES = new long[1_000_000];
    private static final byte[] FLAGS = new byte[FRAMES.length];
    private static final List<String> SYSTEM = new ArrayList<>();
    private static final List<GarbageCollectorMXBean> GC = ManagementFactory.getGarbageCollectorMXBeans();
    private static Path output;
    private static String label = "capture";
    private static int warmup = 60, seconds = 60, count;
    private static long entered, started, last, lastCpu = -1, lastSample;
    private static volatile boolean finished, recording;
    private static boolean written;
    private static boolean previousWorld, previousGui;
    private static Field world, screen;
    private static ScheduledExecutorService worker;
    public static void initialize(String arguments) {
        output = Paths.get("lilt-probe-results").toAbsolutePath();
        if (arguments != null) for (String part : arguments.split(";")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) throw new IllegalArgumentException("Probe arguments must be key=value, separated by semicolons");
            if (pair[0].equals("output")) output = Paths.get(pair[1]).toAbsolutePath();
            else if (pair[0].equals("label")) label = pair[1].replaceAll("[^A-Za-z0-9_-]", "_");
            else if (pair[0].equals("warmup")) warmup = Integer.parseInt(pair[1]);
            else if (pair[0].equals("seconds")) seconds = Integer.parseInt(pair[1]);
            else throw new IllegalArgumentException("Unknown probe argument: " + pair[0]);
        }
        if (warmup < 0 || warmup > 3600 || seconds < 1 || seconds > 3600) throw new IllegalArgumentException("Invalid probe duration");
        SYSTEM.add("elapsed_seconds,heap_used_bytes,heap_committed_bytes,process_cpu_percent_of_machine,gc_count_total,gc_millis_total");
        worker = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "Lilt baseline probe"); t.setDaemon(true); return t; });
        worker.scheduleAtFixedRate(() -> { if (finished) write(); else if (recording) sample(); }, 1, 1, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { finished = true; write(); worker.shutdown(); }, "Lilt probe shutdown"));
        System.out.println("[Lilt probe] " + label + ": warmup " + warmup + " s, capture " + seconds + " s. Output: " + output);
    }
    public static void frame(Object minecraft) {
        if (finished) return;
        long now = System.nanoTime();
        try {
            if (world == null) {
                world = field(minecraft.getClass(), "world", "field_71441_e", "f");
                screen = field(minecraft.getClass(), "currentScreen", "field_71462_r", "m");
            }
            boolean inWorld = world.get(minecraft) != null, gui = screen.get(minecraft) != null;
            if (entered == 0 && inWorld && !gui) { entered = now; System.out.println("[Lilt probe] World entered; warming up."); }
            if (!recording && entered != 0 && now - entered >= warmup * 1_000_000_000L && inWorld && !gui) {
                started = last = now; previousWorld = inWorld; previousGui = gui;
                recording = true; System.out.println("[Lilt probe] Recording."); return;
            }
            if (!recording) return;
            long interval = now - last; last = now;
            if (interval > 0) { FRAMES[count] = interval; FLAGS[count] = (byte) ((inWorld && previousWorld ? 1 : 0) | (gui || previousGui ? 2 : 0)); count++; }
            previousWorld = inWorld; previousGui = gui;
            if (count == FRAMES.length || now - started >= seconds * 1_000_000_000L) { recording = false; finished = true; }
        } catch (ReflectiveOperationException failure) { finished = true; System.err.println("[Lilt probe] Unsupported field signature: " + failure); }
    }
    private static Field field(Class<?> type, String... names) throws NoSuchFieldException {
        for (String name : names) try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; } catch (NoSuchFieldException ignored) { }
        throw new NoSuchFieldException(Arrays.toString(names));
    }
    private static synchronized void sample() {
        Runtime runtime = Runtime.getRuntime();
        long gcCount = 0, gcMillis = 0;
        for (GarbageCollectorMXBean bean : GC) { gcCount += Math.max(0, bean.getCollectionCount()); gcMillis += Math.max(0, bean.getCollectionTime()); }
        long now = System.nanoTime(); double cpuPercent = -1;
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof com.sun.management.OperatingSystemMXBean) {
            long cpu = ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuTime();
            if (lastCpu >= 0 && now > lastSample) cpuPercent = 100.0 * (cpu - lastCpu) / (now - lastSample) / os.getAvailableProcessors();
            lastCpu = cpu; lastSample = now;
        }
        SYSTEM.add(String.format(Locale.ROOT, "%.6f,%d,%d,%.4f,%d,%d", (now - started) / 1e9,
            runtime.totalMemory() - runtime.freeMemory(), runtime.totalMemory(), cpuPercent, gcCount, gcMillis));
    }
    private static synchronized void write() {
        if (written || count == 0) return;
        written = true;
        try {
            Files.createDirectories(output);
            Path frames = output.resolve(label + "-frames.csv");
            try (BufferedWriter writer = Files.newBufferedWriter(frames, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
                writer.write("frame,frame_time_ns,in_world,menu_open\n");
                for (int i = 0; i < count; i++) writer.write(i + "," + FRAMES[i] + "," + ((FLAGS[i] & 1) != 0) + "," + ((FLAGS[i] & 2) != 0) + "\n");
            }
            Files.write(output.resolve(label + "-system.csv"), SYSTEM, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            System.out.println("[Lilt probe] Saved " + count + " intervals to " + frames);
        } catch (IOException failure) { System.err.println("[Lilt probe] Write failed (existing files are never overwritten): " + failure); }
    }
}
