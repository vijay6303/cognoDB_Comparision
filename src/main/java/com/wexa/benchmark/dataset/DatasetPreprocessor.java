package com.wexa.benchmark.dataset;

import java.io.*;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

public class DatasetPreprocessor {

    private static final long TARGET_RELATIONSHIPS = 200_000;

    public static void main(String[] args) {

        String inputFile = "data/soc-pokec-relationships.txt";
        String outputFile = "data/soc-Pokec-200k.txt";

        process(inputFile, outputFile);
    }

    public static void process(String inputFile, String outputFile) {

        long relationships = 0;
        long duplicates = 0;
        long selfLoops = 0;

        Set<String> seenEdges = new HashSet<>();
        Set<Long> nodes = new HashSet<>();

        Path input = Path.of(inputFile);
        Path output = Path.of(outputFile);

        try (
                BufferedReader reader = Files.newBufferedReader(input);
                BufferedWriter writer = Files.newBufferedWriter(
                        output,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                )
        ) {

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");

                if (parts.length < 2) {
                    continue;
                }

                long source = Long.parseLong(parts[0]);
                long destination = Long.parseLong(parts[1]);

                // Ignore self-loops
                if (source == destination) {
                    selfLoops++;
                    continue;
                }

                String edge = source + "," + destination;

                // Ignore duplicate edges
                if (!seenEdges.add(edge)) {
                    duplicates++;
                    continue;
                }

                writer.write(source + " " + destination);
                writer.newLine();

                nodes.add(source);
                nodes.add(destination);

                relationships++;

                if (relationships >= TARGET_RELATIONSHIPS) {
                    break;
                }
            }

            System.out.println("Preprocessing completed.");
            System.out.println("Relationships written: " + relationships);
            System.out.println("Unique nodes: " + nodes.size());
            System.out.println("Duplicates skipped: " + duplicates);
            System.out.println("Self-loops skipped: " + selfLoops);
            System.out.println("Output: " + outputFile);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to process dataset",
                    e
            );
        }
    }
}