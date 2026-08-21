package com.wexa.benchmark.benchmark;

import com.wexa.benchmark.client.CognoDbClient;
import com.wexa.benchmark.metrics.BenchmarkResult;
import com.wexa.benchmark.metrics.CsvResultWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FinalTraversalBenchmark {

    private final CognoDbClient client;
    private final CsvResultWriter writer;

    public FinalTraversalBenchmark(
            CognoDbClient client,
            CsvResultWriter writer) {

        this.client = client;
        this.writer = writer;
    }

    public void run(
            List<Long> userIds,
            int warmupIterations,
            int measuredIterations) {

        for (long userId : userIds) {

            System.out.println();
            System.out.println(
                    "Benchmarking user: " + userId
            );

            for (int hops = 1; hops <= 3; hops++) {

                System.out.println(
                        "  Running " + hops + "-hop..."
                );

                // -------------------------
                // Warm-up
                // -------------------------

                for (int i = 0;
                     i < warmupIterations;
                     i++) {

                    client.runTraversal(
                            userId,
                            hops
                    );
                }

                // -------------------------
                // Measurement
                // -------------------------

                List<Long> latencies =
                        new ArrayList<>();

                int successful = 0;
                int failed = 0;

                for (int i = 0;
                     i < measuredIterations;
                     i++) {

                    long start =
                            System.nanoTime();

                    try {

                        client.runTraversal(
                                userId,
                                hops
                        );

                        successful++;

                    } catch (Exception e) {

                        failed++;

                        System.err.println(
                                "Query failed for user "
                                        + userId
                                        + ", hops "
                                        + hops
                        );

                    } finally {

                        long elapsed =
                                System.nanoTime()
                                        - start;

                        latencies.add(
                                elapsed / 1_000
                        );
                    }
                }

                Collections.sort(latencies);

                double average =
                        latencies.stream()
                                .mapToLong(
                                        Long::longValue
                                )
                                .average()
                                .orElse(0);

                long min =
                        latencies.isEmpty()
                                ? 0
                                : latencies.get(0);

                long p50 =
                        percentile(
                                latencies,
                                50
                        );

                long p95 =
                        percentile(
                                latencies,
                                95
                        );

                long max =
                        latencies.isEmpty()
                                ? 0
                                : latencies.get(
                                        latencies.size() - 1
                                );

                BenchmarkResult result =
                        new BenchmarkResult(
                                "traversal",
                                hops,
                                userId,
                                successful,
                                failed,
                                average,
                                min,
                                p50,
                                p95,
                                max
                        );

                writer.write(result);

                printResult(
                        userId,
                        hops,
                        result
                );
            }
        }
    }

    private long percentile(
            List<Long> values,
            int percentile) {

        if (values.isEmpty()) {
            return 0;
        }

        int index =
                (int) Math.ceil(
                        percentile / 100.0
                                * values.size()
                ) - 1;

        index =
                Math.max(
                        0,
                        Math.min(
                                index,
                                values.size() - 1
                        )
                );

        return values.get(index);
    }

    private void printResult(
            long userId,
            int hops,
            BenchmarkResult result) {

        System.out.println(
                "    User: " + userId
                        + " | Hops: " + hops
        );

        System.out.println(
                "    Result: " + result.toCsv()
        );
    }
}