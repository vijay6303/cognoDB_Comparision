package com.wexa.benchmark.benchmark;

import com.wexa.benchmark.client.GraphDatabaseClient;
import com.wexa.benchmark.metrics.ConcurrentBenchmarkResult;
import com.wexa.benchmark.metrics.ConcurrentCsvResultWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentReadWriteBenchmark {

        private final GraphDatabaseClient client;
    private final ConcurrentCsvResultWriter writer;

    public ConcurrentReadWriteBenchmark(
            GraphDatabaseClient client,
            ConcurrentCsvResultWriter writer) {

        this.client = client;
        this.writer = writer;
    }

    public void run(
            long userId,
            long targetId,
            int readerThreads,
            int operationsPerReader,
            int writerOperations) {

        System.out.println();
        System.out.println(
                "======================================"
        );

        System.out.println(
                "Concurrent Reads/Writes Benchmark"
        );

        System.out.println(
                "======================================"
        );

        System.out.println(
                "Reader threads: " + readerThreads
        );

        System.out.println(
                "Writer threads: 1"
        );

        System.out.println(
                "Operations per reader: "
                        + operationsPerReader
        );

        System.out.println(
                "Writer operations: "
                        + writerOperations
        );

        int totalOperations =
                (readerThreads * operationsPerReader)
                        + writerOperations;

        AtomicInteger successful =
                new AtomicInteger();

        AtomicInteger failed =
                new AtomicInteger();

        List<Long> latenciesMicros =
                Collections.synchronizedList(
                        new ArrayList<>()
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        readerThreads + 1
                );

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch finishLatch =
                new CountDownLatch(
                        readerThreads + 1
                );

        // --------------------------------
        // Reader threads
        // --------------------------------

        for (int i = 0;
             i < readerThreads;
             i++) {

            executor.submit(() -> {

                try {

                    startLatch.await();

                    for (int j = 0;
                         j < operationsPerReader;
                         j++) {

                        long start =
                                System.nanoTime();

                        try {

                            client.runConcurrentRead(
                                    userId
                            );

                            successful.incrementAndGet();

                        } catch (Exception e) {

                            failed.incrementAndGet();

                        } finally {

                            long elapsed =
                                    System.nanoTime()
                                            - start;

                            latenciesMicros.add(
                                    elapsed / 1_000
                            );
                        }
                    }

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                } finally {

                    finishLatch.countDown();
                }
            });
        }

        // --------------------------------
        // Writer thread
        // --------------------------------

        executor.submit(() -> {

            try {

                startLatch.await();

                for (int i = 0;
                     i < writerOperations;
                     i++) {

                    long start =
                            System.nanoTime();

                    try {

                        client.runConcurrentWrite(
                                userId,
                                targetId,
                                i
                        );

                        successful.incrementAndGet();

                    } catch (Exception e) {

                        failed.incrementAndGet();

                    } finally {

                        long elapsed =
                                System.nanoTime()
                                        - start;

                        latenciesMicros.add(
                                elapsed / 1_000
                        );
                    }
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

            } finally {

                finishLatch.countDown();
            }
        });

        // --------------------------------
        // Start benchmark
        // --------------------------------

        long startTime =
                System.nanoTime();

        startLatch.countDown();

        try {

            finishLatch.await();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }

        long endTime =
                System.nanoTime();

        executor.shutdown();

        try {

            executor.awaitTermination(
                    10,
                    TimeUnit.SECONDS
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }

        // --------------------------------
        // Calculate metrics
        // --------------------------------

        double elapsedSeconds =
                (endTime - startTime)
                        / 1_000_000_000.0;

        int successfulCount =
                successful.get();

        int failedCount =
                failed.get();

        double throughput =
                successfulCount / elapsedSeconds;

        Collections.sort(latenciesMicros);

        double averageMicros =
                latenciesMicros.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0);

        long minMicros =
                latenciesMicros.isEmpty()
                        ? 0
                        : latenciesMicros.get(0);

        long p50Micros =
                percentile(
                        latenciesMicros,
                        50
                );

        long p95Micros =
                percentile(
                        latenciesMicros,
                        95
                );

        long maxMicros =
                latenciesMicros.isEmpty()
                        ? 0
                        : latenciesMicros.get(
                                latenciesMicros.size() - 1
                        );

        // --------------------------------
        // Print results
        // --------------------------------

        System.out.println();

        System.out.println(
                "Concurrent benchmark completed."
        );

        System.out.println();

        System.out.println(
                "Expected operations: "
                        + totalOperations
        );

        System.out.println(
                "Successful operations: "
                        + successfulCount
        );

        System.out.println(
                "Failed operations: "
                        + failedCount
        );

        System.out.printf(
                "Elapsed time: %.3f seconds%n",
                elapsedSeconds
        );

        System.out.printf(
                "Throughput: %.3f operations/sec%n",
                throughput
        );

        System.out.printf(
                "Average latency: %.3f ms%n",
                averageMicros / 1000.0
        );

        System.out.printf(
                "Min latency: %.3f ms%n",
                minMicros / 1000.0
        );

        System.out.printf(
                "p50 latency: %.3f ms%n",
                p50Micros / 1000.0
        );

        System.out.printf(
                "p95 latency: %.3f ms%n",
                p95Micros / 1000.0
        );

        System.out.printf(
                "Max latency: %.3f ms%n",
                maxMicros / 1000.0
        );

        // --------------------------------
        // CSV
        // --------------------------------

        ConcurrentBenchmarkResult result =
                new ConcurrentBenchmarkResult(
                        readerThreads,
                        1,
                        totalOperations,
                        successfulCount,
                        failedCount,
                        elapsedSeconds,
                        throughput,
                        averageMicros,
                        minMicros,
                        p50Micros,
                        p95Micros,
                        maxMicros
                );

        writer.write(result);

        System.out.println();
        System.out.println(
                "Result written to:"
        );
        System.out.println(
                "results/concurrent_read_write.csv"
        );

        System.out.println(
                "======================================"
        );
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
}