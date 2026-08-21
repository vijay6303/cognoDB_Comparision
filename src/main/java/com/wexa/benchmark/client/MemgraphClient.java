package com.wexa.benchmark.client;

public class MemgraphClient
        extends Neo4jClient {

    public MemgraphClient() {

        super(
            "bolt://localhost:17688",
                "",
                ""
        );
    }

    @Override
    public void createIndexes() {
        // Memgraph's Bolt adapter rejects schema changes in this transaction path.
        // Point lookups remain valid as filtered lookups and are reported as such.
    }
}