package dev.lilt.metrics;

import dev.lilt.config.ConfigStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/** Game-thread producer, detached capture handed to a single background writer on stop. */
public final class BenchmarkRecorder implements AutoCloseable {
    private static final int MAX_FRAMES = 1_000_000;
    private final Path directory;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Lilt capture writer"); thread.setDaemon(true); return thread;
    });
    private Capture capture;
    public volatile String status = "Ready to record";
    public volatile Path lastReport;
    private volatile boolean writing;
    public BenchmarkRecorder(Path directory) { this.directory = directory; }
    public boolean active() { return capture != null; }
    public int frameCount() { return capture == null ? 0 : capture.count; }
    public double elapsedSeconds(long now) { return capture == null ? 0 : (now - capture.startedNanos) / 1e9; }
    public boolean start(String scenario, int durationSeconds, Map<String, Object> metadata, SystemMetrics system) {
        if (active() || writing) return false;
        capture = new Capture(scenario, Math.max(10, Math.min(3600, durationSeconds)), metadata, system);
        status = "Recording " + scenario;
        return true;
    }
    public void frame(long interval, boolean inWorld, boolean menuOpen, long now, SystemMetrics system) {
        Capture current = capture;
        if (current == null) return;
        // The first observed interval started before the capture; exclude it.
        if (!current.firstObserved) {
            current.firstObserved = true; current.startedNanos = now;
            current.previousWorld = inWorld; current.previousMenu = menuOpen; return;
        }
        if (interval > 0 && current.count < MAX_FRAMES) {
            current.frames[current.count] = interval;
            current.flags[current.count] = (byte) ((inWorld && current.previousWorld ? 1 : 0) | (menuOpen || current.previousMenu ? 2 : 0));
            current.count++;
        }
        current.previousWorld = inWorld; current.previousMenu = menuOpen;
        if (now - current.lastSystemSample >= 1_000_000_000L) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("elapsedSeconds", (now - current.startedNanos) / 1e9);
            row.put("heapUsedBytes", system.heapUsedBytes);
            row.put("heapCommittedBytes", system.heapCommittedBytes);
            row.put("processCpuPercentOfMachine", system.processCpuPercent);
            row.put("gcCollectionsSinceStart", system.gcCollections - current.gcStart);
            row.put("gcMillisSinceStart", system.gcMillis - current.gcMillisStart);
            current.system.add(row);
            current.lastSystemSample = now;
        }
        if (current.count == MAX_FRAMES || now - current.startedNanos >= current.durationSeconds * 1_000_000_000L) {
            stop(current.count == MAX_FRAMES ? "frame capacity reached" : "duration reached");
        }
    }
    public void stop(String reason) {
        final Capture finished = capture;
        if (finished == null) return;
        capture = null;
        writing = true;
        status = "Writing capture…";
        writer.submit(() -> {
            try { write(finished, reason); status = "Saved " + lastReport.getFileName(); }
            catch (IOException | RuntimeException failure) { status = "Capture failed: " + failure.getMessage(); }
            finally { writing = false; }
        });
    }
    private void write(Capture capture, String reason) throws IOException {
        Files.createDirectories(directory);
        String prefix = DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC).format(capture.started)
            + "-" + capture.scenario.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        Path framesPath = directory.resolve(prefix + "-frames.csv");
        try (BufferedWriter out = Files.newBufferedWriter(framesPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            out.write("frame,frame_time_ns,in_world,menu_open\n");
            for (int i = 0; i < capture.count; i++) {
                out.write(i + "," + capture.frames[i] + "," + ((capture.flags[i] & 1) != 0) + "," + ((capture.flags[i] & 2) != 0) + "\n");
            }
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("formatVersion", 1);
        report.put("startedUtc", capture.started.toString());
        report.put("scenario", capture.scenario);
        report.put("stopReason", reason);
        report.put("requestedDurationSeconds", capture.durationSeconds);
        report.put("measurement", "Intervals between Forge RenderTick START events; includes limiter and ticks. Not display presentation or input-to-photon latency.");
        report.put("definitions", "Average FPS = 1e9 / mean(ns). 1% low = 1e9 / mean(slowest ceil(N*0.01) intervals). Percentiles use nearest rank.");
        report.put("allFrames", FrameStatistics.calculate(capture.frames, capture.count));
        int gameplayCount = 0;
        long[] gameplay = new long[capture.count];
        for (int i = 0; i < capture.count; i++) if (capture.flags[i] == 1) gameplay[gameplayCount++] = capture.frames[i];
        report.put("worldWithoutGui", FrameStatistics.calculate(gameplay, gameplayCount));
        report.put("excludedFromGameplayCount", capture.count - gameplayCount);
        report.put("metadata", capture.metadata);
        report.put("systemSamples", capture.system);
        report.put("gpuUtilization", "Not available from portable LWJGL 2 APIs. Use external vendor counters.");
        report.put("inputLatency", "Not measured. Use an external high-speed camera or validated latency instrument.");
        report.put("gcNote", "MXBean aggregate collection count/time, sampled at 1 Hz; not individual stop-the-world pause durations.");
        Path reportPath = directory.resolve(prefix + "-summary.json");
        ConfigStore.atomicWrite(reportPath, ConfigStore.JSON.toJson(report));
        lastReport = reportPath;
    }
    public void close() {
        stop("client shutdown");
        writer.shutdown();
        try { writer.awaitTermination(15, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }
    private static final class Capture {
        final long[] frames = new long[MAX_FRAMES];
        final byte[] flags = new byte[MAX_FRAMES];
        final List<Map<String, Object>> system = new ArrayList<>();
        final String scenario;
        final int durationSeconds;
        final Map<String, Object> metadata;
        final Instant started = Instant.now();
        final long gcStart, gcMillisStart;
        long startedNanos = System.nanoTime(), lastSystemSample;
        int count;
        boolean firstObserved, previousWorld, previousMenu;
        Capture(String scenario, int durationSeconds, Map<String, Object> metadata, SystemMetrics metrics) {
            this.scenario = scenario; this.durationSeconds = durationSeconds;
            this.metadata = new LinkedHashMap<>(metadata);
            gcStart = metrics.gcCollections; gcMillisStart = metrics.gcMillis;
        }
    }
}
