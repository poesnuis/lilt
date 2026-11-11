package dev.lilt.metrics;

import java.util.Arrays;

/** Explicit, reproducible definitions shared by rolling metrics and saved captures. */
public final class FrameStatistics {
    public final int count;
    public final double averageFps, onePercentLowFps, meanMs, p50Ms, p95Ms, p99Ms, maxMs, standardDeviationMs;
    private FrameStatistics(int count, double fps, double low, double mean, double p50, double p95, double p99, double max, double std) {
        this.count = count; averageFps = fps; onePercentLowFps = low; meanMs = mean;
        p50Ms = p50; p95Ms = p95; p99Ms = p99; maxMs = max; standardDeviationMs = std;
    }
    public static FrameStatistics calculate(long[] nanos, int count) {
        if (count < 0 || count > nanos.length) throw new IllegalArgumentException("Invalid sample count");
        if (count == 0) return new FrameStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0);
        long[] sorted = Arrays.copyOf(nanos, count);
        double total = 0;
        for (long value : sorted) {
            if (value <= 0) throw new IllegalArgumentException("Frame times must be positive");
            total += value;
        }
        Arrays.sort(sorted);
        double mean = total / count;
        int tailCount = Math.max(1, (int) Math.ceil(count * .01));
        double tail = 0, variance = 0;
        for (int i = count - tailCount; i < count; i++) tail += sorted[i];
        for (long value : sorted) { double delta = value - mean; variance += delta * delta; }
        return new FrameStatistics(count, 1e9 / mean, 1e9 / (tail / tailCount), mean / 1e6,
            percentile(sorted, .50), percentile(sorted, .95), percentile(sorted, .99),
            sorted[count - 1] / 1e6, Math.sqrt(variance / count) / 1e6);
    }
    private static double percentile(long[] sorted, double fraction) {
        return sorted[Math.max(0, (int) Math.ceil(sorted.length * fraction) - 1)] / 1e6;
    }
}
