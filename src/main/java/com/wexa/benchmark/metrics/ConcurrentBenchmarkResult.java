package com.wexa.benchmark.metrics;

public class ConcurrentBenchmarkResult {

    private final int readerThreads;
    private final int writerThreads;
    private final int totalOperations;
    private final int successfulOperations;
    private final int failedOperations;

    private final double elapsedSeconds;
    private final double throughput;

    private final double averageMicros;
    private final long minMicros;
    private final long p50Micros;
    private final long p95Micros;
    private final long maxMicros;

    public ConcurrentBenchmarkResult(
            int readerThreads,
            int writerThreads,
            int totalOperations,
            int successfulOperations,
            int failedOperations,
            double elapsedSeconds,
            double throughput,
            double averageMicros,
            long minMicros,
            long p50Micros,
            long p95Micros,
            long maxMicros) {

        this.readerThreads = readerThreads;
        this.writerThreads = writerThreads;
        this.totalOperations = totalOperations;
        this.successfulOperations = successfulOperations;
        this.failedOperations = failedOperations;
        this.elapsedSeconds = elapsedSeconds;
        this.throughput = throughput;
        this.averageMicros = averageMicros;
        this.minMicros = minMicros;
        this.p50Micros = p50Micros;
        this.p95Micros = p95Micros;
        this.maxMicros = maxMicros;
    }

    public String toCsv() {

        return String.format(
                "%d,%d,%d,%d,%d,%.3f,%.3f,%.3f,%d,%d,%d,%d",
                readerThreads,
                writerThreads,
                totalOperations,
                successfulOperations,
                failedOperations,
                elapsedSeconds,
                throughput,
                averageMicros,
                minMicros,
                p50Micros,
                p95Micros,
                maxMicros
        );
    }
}