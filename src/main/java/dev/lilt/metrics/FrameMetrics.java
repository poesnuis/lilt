package dev.lilt.metrics;

public final class FrameMetrics {
    private final long[] frames = new long[240];
    private int cursor, count;
    private long last, lastSummary;
    private FrameStatistics summary = FrameStatistics.calculate(frames, 0);
    /** Returns the actual interval including ticking, presentation and the limiter. */
    public long record(long now) {
        long delta = last == 0 ? 0 : now - last;
        last = now;
        if (delta > 0) {
            frames[cursor] = delta;
            cursor = (cursor + 1) % frames.length;
            count = Math.min(count + 1, frames.length);
        }
        if (now - lastSummary > 1_000_000_000L) {
            summary = FrameStatistics.calculate(frames, count);
            lastSummary = now;
        }
        return delta;
    }
    public FrameStatistics summary() { return summary; }
    public int size() { return count; }
    public double recentMs(int age) {
        if (age < 0 || age >= count) return 0;
        return frames[(cursor - 1 - age + frames.length) % frames.length] / 1e6;
    }
}
