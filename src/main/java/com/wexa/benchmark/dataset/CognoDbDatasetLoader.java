package com.wexa.benchmark.dataset;

import com.wexa.benchmark.client.GraphDatabaseClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CognoDbDatasetLoader {

    private final GraphDatabaseClient client;

    public CognoDbDatasetLoader(GraphDatabaseClient client) {
        this.client = client;
    }

    public void load(String filePath) {

        long relationshipsLoaded = 0;
        long startTime = System.nanoTime();
        int batchSize = Integer.parseInt(System.getenv()
            .getOrDefault("BENCHMARK_BATCH_SIZE", "100"));
        long maximumRelationships = Long.parseLong(System.getenv()
            .getOrDefault("BENCHMARK_MAX_RELATIONSHIPS", "200000"));

        List<Long> sources = new ArrayList<>(batchSize);
        List<Long> destinations = new ArrayList<>(batchSize);

        try (BufferedReader reader =
                     Files.newBufferedReader(Path.of(filePath))) {

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

                if (relationshipsLoaded + sources.size() >= maximumRelationships) {
                    break;
                }

                sources.add(source);
                destinations.add(destination);

                if (sources.size() == batchSize) {

                    client.addRelationships(sources, destinations);

                    relationshipsLoaded += sources.size();

                    sources.clear();
                    destinations.clear();

                    System.out.println(
                            "Loaded relationships: "
                                    + relationshipsLoaded
                    );
                }
            }

            // Load remaining relationships
            if (!sources.isEmpty()) {

                client.addRelationships(sources, destinations);

                relationshipsLoaded += sources.size();
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to read dataset",
                    e
            );
        }

        long endTime = System.nanoTime();

        double seconds =
                (endTime - startTime) / 1_000_000_000.0;

        System.out.println();
        System.out.println(
                "Relationships loaded: "
                        + relationshipsLoaded
        );

        System.out.println(
                "Load time: "
                        + seconds
                        + " seconds"
        );

        System.out.println(
                "Relationships/sec: "
                        + (relationshipsLoaded / seconds)
        );
    }
}