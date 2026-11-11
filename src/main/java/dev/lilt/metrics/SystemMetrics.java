package dev.lilt.metrics;

import java.lang.management.*;
import java.util.List;

public final class SystemMetrics {
    private final List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
    private final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
    public long heapUsedBytes, heapCommittedBytes, heapMaxBytes, gcCollections, gcMillis;
    /** Whole-machine share: 100% means all logical CPUs. -1 means unavailable. */
    public double processCpuPercent = -1;
    private long previousCpu = -1, previousTime;

    public void sample(long now) {
        Runtime runtime = Runtime.getRuntime();
        heapCommittedBytes = runtime.totalMemory();
        heapUsedBytes = heapCommittedBytes - runtime.freeMemory();
        heapMaxBytes = runtime.maxMemory();
        gcCollections = 0; gcMillis = 0;
        for (GarbageCollectorMXBean bean : collectors) {
            gcCollections += Math.max(0, bean.getCollectionCount());
            gcMillis += Math.max(0, bean.getCollectionTime());
        }
        if (os instanceof com.sun.management.OperatingSystemMXBean) {
            long cpu = ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuTime();
            if (previousCpu >= 0 && now > previousTime && cpu >= previousCpu) {
                processCpuPercent = Math.max(0, Math.min(100, 100.0 * (cpu - previousCpu) / (now - previousTime) / os.getAvailableProcessors()));
            }
            previousCpu = cpu;
            previousTime = now;
        }
    }
}
