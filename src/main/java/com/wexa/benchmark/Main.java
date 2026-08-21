package com.wexa.benchmark;

import com.wexa.benchmark.benchmark.AggregationBenchmark;
import com.wexa.benchmark.benchmark.ConcurrentReadWriteBenchmark;
import com.wexa.benchmark.benchmark.PointLookupBenchmark;
import com.wexa.benchmark.benchmark.TraversalBenchmark;
import com.wexa.benchmark.client.ArcadeDbClient;
import com.wexa.benchmark.client.CognoDbClient;
import com.wexa.benchmark.client.FalkorDbClient;
import com.wexa.benchmark.client.GraphDatabaseClient;
import com.wexa.benchmark.client.MemgraphClient;
import com.wexa.benchmark.client.Neo4jClient;
import com.wexa.benchmark.config.DatabaseConfig;
import com.wexa.benchmark.dataset.CognoDbDatasetLoader;
import com.wexa.benchmark.metrics.ConcurrentCsvResultWriter;
import com.wexa.benchmark.metrics.CsvResultWriter;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        String database = System.getenv()
                .getOrDefault("BENCHMARK_DATABASE", "cognodb")
                .toLowerCase();
        GraphDatabaseClient client = createClient(database);
        String resultDirectory = System.getenv().getOrDefault(
                "BENCHMARK_OUTPUT_DIR", "results/" + database);
        String dataset = System.getenv().getOrDefault(
                "BENCHMARK_DATASET", "data/soc-Pokec-200k.txt");
        boolean forceLoad = Boolean.parseBoolean(System.getenv()
                .getOrDefault("BENCHMARK_FORCE_LOAD", "false"));

        int warmup = Integer.parseInt(
                System.getenv().getOrDefault("BENCHMARK_WARMUP", "10"));
        int iterations = Integer.parseInt(
                System.getenv().getOrDefault("BENCHMARK_ITERATIONS", "100"));
        int readerThreads = Integer.parseInt(
                System.getenv().getOrDefault("BENCHMARK_READERS", "10"));
        int operationsPerReader = Integer.parseInt(
                System.getenv().getOrDefault("BENCHMARK_READ_OPERATIONS", "100"));
        int writerOperations = Integer.parseInt(
                System.getenv().getOrDefault("BENCHMARK_WRITE_OPERATIONS", "100"));

        boolean preserveExisting = Boolean.parseBoolean(System.getenv()
                .getOrDefault("BENCHMARK_PRESERVE_RESULTS", "false"));
        CsvResultWriter traversalWriter = writer(resultDirectory + "/traversal.csv", preserveExisting);
        CsvResultWriter pointLookupWriter = writer(resultDirectory + "/point_lookup.csv", preserveExisting);
        CsvResultWriter aggregationWriter = writer(resultDirectory + "/aggregation.csv", preserveExisting);
        ConcurrentCsvResultWriter concurrentWriter =
                new ConcurrentCsvResultWriter(resultDirectory + "/concurrent_read_write.csv");
        if (!preserveExisting) {
            concurrentWriter.initialize();
        }

        try {

            client.connect();

                        if (forceLoad || client.countRelationships() == 0) {
                                System.out.println("Loading dataset into " + database + ": " + dataset);
                                new CognoDbDatasetLoader(client).load(dataset);
                        }

            client.createIndexes();

            System.out.println("Nodes: " + client.countNodes());
            long relationshipCount = client.countRelationships();
            System.out.println("Relationships: " + relationshipCount);
            writeMetadata(resultDirectory, database, relationshipCount, warmup, iterations);

            /*
             * Select deterministic representative users.
             */
            List<Long> userIds =
                    client.getRepresentativeUserIds();

            System.out.println();
            System.out.println(
                    "Representative users:"
            );

            userIds.forEach(
                    id -> System.out.println(
                            "  " + id
                    )
            );

            if (userIds.size() < 2) {
                throw new IllegalStateException(
                        "At least two representative users are required.");
            }

            TraversalBenchmark traversal =
                    new TraversalBenchmark(client, traversalWriter);
            for (int hops = 1; hops <= 3; hops++) {
                traversal.run(hops, userIds.get(0), warmup, iterations);
            }

            new PointLookupBenchmark(client, pointLookupWriter)
                    .run(userIds.get(0), warmup, iterations);

            AggregationBenchmark aggregation =
                    new AggregationBenchmark(client, aggregationWriter);
            aggregation.run("user_count", warmup, iterations);
            aggregation.run("relationship_count", warmup, iterations);
            aggregation.run("top_degrees", warmup, iterations);

            new ConcurrentReadWriteBenchmark(client, concurrentWriter)
                    .run(userIds.get(0), userIds.get(1), readerThreads,
                            operationsPerReader, writerOperations);

        } finally {

            client.close();
        }
    }

        private static GraphDatabaseClient createClient(String database) {
                return switch (database) {
                        case "cognodb" -> new CognoDbClient(new DatabaseConfig());
                        case "neo4j" -> new Neo4jClient(
                                        "bolt://localhost:17687", "neo4j", "benchmark123");
                        case "memgraph" -> new MemgraphClient();
                        case "arcadedb" -> new ArcadeDbClient();
                            case "falkordb" -> new FalkorDbClient();
                        default -> throw new IllegalArgumentException(
                                        "Unknown BENCHMARK_DATABASE: " + database
                                                            + ". Use cognodb, neo4j, memgraph, arcadedb, or falkordb.");
                };
        }

        private static CsvResultWriter writer(String path, boolean preserveExisting) {
                CsvResultWriter writer = new CsvResultWriter(path);
                if (!preserveExisting) {
                        writer.initialize();
                }
                return writer;
        }

        private static void writeMetadata(
                String resultDirectory,
                String database,
                long relationshipCount,
                int warmup,
                int iterations) {
                try {
                        Files.createDirectories(Path.of(resultDirectory));
                        Files.writeString(
                                Path.of(resultDirectory, "run-metadata.txt"),
                                "database=" + database + System.lineSeparator()
                                        + "relationships=" + relationshipCount + System.lineSeparator()
                                        + "warmup=" + warmup + System.lineSeparator()
                                        + "iterations=" + iterations + System.lineSeparator());
                } catch (IOException e) {
                        throw new IllegalStateException("Unable to write benchmark metadata", e);
                }
        }
}