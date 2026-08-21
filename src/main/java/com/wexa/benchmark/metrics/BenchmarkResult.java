package com.wexa.benchmark.metrics;

public class BenchmarkResult {

    private final String workload;
    private final int hops;
    private final long userId;
    private final int successfulQueries;
    private final int failedQueries;

    private final double averageMicros;
    private final long minMicros;
    private final long p50Micros;
    private final long p95Micros;
    private final long maxMicros;

    public BenchmarkResult(
            String workload,
            int hops,
            long userId,
            int successfulQueries,
            int failedQueries,
            double averageMicros,
            long minMicros,
            long p50Micros,
            long p95Micros,
            long maxMicros) {

        this.workload = workload;
        this.hops = hops;
        this.userId = userId;
        this.successfulQueries = successfulQueries;
        this.failedQueries = failedQueries;
        this.averageMicros = averageMicros;
        this.minMicros = minMicros;
        this.p50Micros = p50Micros;
        this.p95Micros = p95Micros;
        this.maxMicros = maxMicros;
    }

    public String toCsv() {

        return String.format(
                "%s,%d,%d,%d,%d,%.2f,%d,%d,%d,%d",
                workload,
                hops,
                userId,
                successfulQueries,
                failedQueries,
                averageMicros,
                minMicros,
                p50Micros,
                p95Micros,
                maxMicros
        );
    }
}