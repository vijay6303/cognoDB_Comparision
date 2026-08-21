package com.wexa.benchmark.metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ConcurrentCsvResultWriter {

    private static final String HEADER =
            "readerThreads,writerThreads,totalOperations,"
            + "successfulOperations,failedOperations,"
            + "elapsedSeconds,throughput,averageMicros,"
            + "minMicros,p50Micros,p95Micros,maxMicros";

    private final Path outputPath;

    public ConcurrentCsvResultWriter(String filePath) {

        this.outputPath = Path.of(filePath);
    }

    public void initialize() {

        try {

            Files.createDirectories(
                    outputPath.getParent()
            );

            Files.writeString(
                    outputPath,
                    HEADER + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to initialize concurrent CSV file",
                    e
            );
        }
    }

    public void write(
            ConcurrentBenchmarkResult result) {

        try {

            Files.writeString(
                    outputPath,
                    result.toCsv()
                            + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to write concurrent benchmark result",
                    e
            );
        }
    }
}