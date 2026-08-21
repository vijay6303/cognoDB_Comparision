package com.wexa.benchmark.client;

public class ArcadeDbClient
        extends Neo4jClient {

    public ArcadeDbClient() {

        super(
                "bolt://localhost:7689",
                "root",
                "benchmark123"
        );
    }

    @Override
    public void createIndexes() {
        // ArcadeDB's Bolt compatibility layer does not support Neo4j index DDL.
    }
}