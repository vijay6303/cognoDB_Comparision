package com.wexa.benchmark.dataset;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DatasetLoader {

    private final Path filePath;

    public DatasetLoader(String filePath) {
        this.filePath = Path.of(filePath);
    }

    public void inspect() {

        long relationships = 0;
        long maxNodeId = 0;

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Skip comments
                if (line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");

                if (parts.length < 2) {
                    continue;
                }

                long source = Long.parseLong(parts[0]);
                long destination = Long.parseLong(parts[1]);

                relationships++;

                maxNodeId = Math.max(
                        maxNodeId,
                        Math.max(source, destination)
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to read dataset: " + filePath,
                    e
            );
        }

        System.out.println(
                "Relationships: " + relationships
        );

        System.out.println(
                "Approx. maximum node ID: " + maxNodeId
        );
    }
}