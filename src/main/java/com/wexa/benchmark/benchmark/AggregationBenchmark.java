package com.wexa.benchmark.benchmark;

import com.wexa.benchmark.client.GraphDatabaseClient;
import com.wexa.benchmark.metrics.BenchmarkResult;
import com.wexa.benchmark.metrics.CsvResultWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AggregationBenchmark {

        private final GraphDatabaseClient client;
    private final CsvResultWriter writer;

    public AggregationBenchmark(
            GraphDatabaseClient client,
            CsvResultWriter writer) {

        this.client = client;
        this.writer = writer;
    }

    public BenchmarkResult run(
            String workload,
            int warmup,
            int iterations) {

        System.out.println();
        System.out.println("=================================");
        System.out.println(
                "Aggregation: " + workload
        );
        System.out.println("=================================");

        for (int i = 0; i < warmup; i++) {
            execute(workload);
        }

        List<Long> latencies = new ArrayList<>();

        int failures = 0;

        for (int i = 0; i < iterations; i++) {

            try {

                long start = System.nanoTime();

                execute(workload);

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
                    "All aggregation queries failed"
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
                        "aggregation_" + workload,
                        0,
                        0,
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

    private void execute(String workload) {

        switch (workload) {

            case "user_count":
                client.runUserCount();
                break;

            case "relationship_count":
                client.runRelationshipCount();
                break;

            case "top_degrees":
                client.runTopDegrees();
                break;

            default:
                throw new IllegalArgumentException(
                        "Unknown aggregation: "
                                + workload
                );
        }
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