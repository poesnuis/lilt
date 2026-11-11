package dev.lilt.metrics;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class FrameStatisticsTest {
    @Test public void averageUsesElapsedTimeAndLowUsesSlowestOnePercentMean() {
        long[] frames = new long[200]; Arrays.fill(frames, 10_000_000L);
        frames[198] = 30_000_000; frames[199] = 50_000_000;
        FrameStatistics stats = FrameStatistics.calculate(frames, frames.length);
        assertEquals(200.0 / 2.06, stats.averageFps, .000001);
        assertEquals(25, stats.onePercentLowFps, .000001);
        assertEquals(10, stats.p99Ms, .000001);
        assertEquals(50, stats.maxMs, .000001);
        assertEquals(30_000_000L, frames[198]);
    }
    @Test public void emptyAndSingleFrameCapturesHaveDefinedResults() {
        assertEquals(0, FrameStatistics.calculate(new long[0], 0).averageFps, 0);
        FrameStatistics stats = FrameStatistics.calculate(new long[]{20_000_000}, 1);
        assertEquals(50, stats.averageFps, 0); assertEquals(50, stats.onePercentLowFps, 0);
        assertEquals(0, stats.standardDeviationMs, 0);
    }
    @Test(expected = IllegalArgumentException.class) public void invalidIntervalsAreNotSilentlyDiscarded() {
        FrameStatistics.calculate(new long[]{0, 1}, 2);
    }
}
