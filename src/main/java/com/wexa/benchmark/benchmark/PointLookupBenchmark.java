package com.wexa.benchmark.benchmark;

import com.wexa.benchmark.client.GraphDatabaseClient;
import com.wexa.benchmark.metrics.BenchmarkResult;
import com.wexa.benchmark.metrics.CsvResultWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PointLookupBenchmark {

        private final GraphDatabaseClient client;
    private final CsvResultWriter writer;

    public PointLookupBenchmark(
            GraphDatabaseClient client,
            CsvResultWriter writer) {

        this.client = client;
        this.writer = writer;
    }

    public BenchmarkResult run(
            long userId,
            int warmup,
            int iterations) {

        System.out.println();
        System.out.println(
                "================================="
        );
        System.out.println(
                "Indexed Point Lookup Benchmark"
        );
        System.out.println(
                "================================="
        );

        System.out.println("User ID: " + userId);
        System.out.println("Warm-up: " + warmup);
        System.out.println("Iterations: " + iterations);

        for (int i = 0; i < warmup; i++) {
            client.runPointLookup(userId);
        }

        List<Long> latencies = new ArrayList<>();

        int failures = 0;

        for (int i = 0; i < iterations; i++) {

            try {

                long start = System.nanoTime();

                client.runPointLookup(userId);

                long end = System.nanoTime();

                latencies.add(
                        (end - start) / 1_000
                );

            } catch (Exception e) {

                failures++;

                System.out.println(
                        "Query failed at iteration "
                                + (i + 1)
                );
            }
        }

        if (latencies.isEmpty()) {
            throw new RuntimeException(
                    "All lookup queries failed"
            );
        }

        Collections.sort(latencies);

        long min = latencies.get(0);

        long max =
                latencies.get(
                        latencies.size() - 1
                );

        long p50 =
                percentile(latencies, 50);

        long p95 =
                percentile(latencies, 95);

        double average =
                latencies.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0);

        BenchmarkResult result =
                new BenchmarkResult(
                        "point_lookup",
                        0,
                        userId,
                        latencies.size(),
                        failures,
                        average,
                        min,
                        p50,
                        p95,
                        max
                );

        System.out.println();
        System.out.println("Results");
        System.out.println("-------------------------");
        System.out.println(result.toCsv());

        writer.write(result);

        return result;
    }

    private long percentile(
            List<Long> values,
            int percentile) {

        int index =
                (int) Math.ceil(
                        percentile / 100.0
                                * values.size()
                ) - 1;

        index = Math.max(
                0,
                Math.min(
                        index,
                        values.size() - 1
                )
        );

        return values.get(index);
    }
}