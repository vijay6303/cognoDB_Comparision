package com.wexa.benchmark.client;

import com.falkordb.Driver;
import com.falkordb.FalkorDB;
import com.falkordb.Graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FalkorDbClient implements GraphDatabaseClient {

    private final Driver driver;
    private final Graph graph;

    public FalkorDbClient() {

        this.driver =
                FalkorDB.driver(
                        "localhost",
                        6379
                );

        this.graph =
                driver.graph("benchmark");
    }

    public void connect() {

        graph.query(
            "RETURN 1"
        );

        System.out.println(
                "Connected to FalkorDB."
        );
    }

    public void close() {

        try {
            driver.close();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to close FalkorDB connection.",
                    e
            );
        }
    }

    @Override
    public void execute(String query) {
        graph.query(query);
    }

    @Override
    public long countNodes() {
        return graph.query("MATCH (n) RETURN count(n) AS count")
                .iterator().next().getValue("count");
    }

    @Override
    public long countRelationships() {
        return graph.query("MATCH ()-[r]->() RETURN count(r) AS count")
                .iterator().next().getValue("count");
    }

    @Override
    public void addRelationships(List<Long> sources, List<Long> destinations) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            rows.add(Map.of("source", sources.get(i), "destination", destinations.get(i)));
        }
        graph.query("""
                UNWIND $rows AS row
                MERGE (a:User {id: row.source})
                MERGE (b:User {id: row.destination})
                MERGE (a)-[:FRIEND]->(b)
                """, Map.of("rows", rows));
    }

    @Override
    public void createIndexes() {
        // FalkorDB uses RedisGraph schema commands rather than Neo4j index DDL.
    }

    @Override
    public List<Long> getRepresentativeUserIds() {
        List<Long> ids = new ArrayList<>();
        for (com.falkordb.Record record : graph.query("""
                MATCH (u:User)-[:FRIEND]->()
                WITH u, count(*) AS degree
                RETURN u.id AS id ORDER BY degree ASC, u.id ASC LIMIT 10
                """)) {
            ids.add(record.getValue("id"));
        }
        return ids;
    }

    @Override
    public void runTraversal(long userId, int hops) {
        if (hops < 1 || hops > 3) {
            throw new IllegalArgumentException("Only 1, 2 and 3 hops are supported.");
        }
        String pattern = "-[:FRIEND]->()".repeat(hops - 1)
                + "-[:FRIEND]->(friend)";
        graph.readOnlyQuery("MATCH (u:User {id: $userId})"
                + pattern + " RETURN count(friend) AS result",
                Map.of("userId", userId));
    }

    @Override
    public void runPointLookup(long userId) {
        graph.readOnlyQuery("MATCH (u:User {id: $userId}) RETURN u.id AS id",
                Map.of("userId", userId));
    }

    @Override
    public void runUserCount() {
        graph.readOnlyQuery("MATCH (u:User) RETURN count(u) AS totalUsers");
    }

    @Override
    public void runRelationshipCount() {
        graph.readOnlyQuery("MATCH ()-[r:FRIEND]->() RETURN count(r) AS totalRelationships");
    }

    @Override
    public void runTopDegrees() {
        graph.readOnlyQuery("""
                MATCH (u:User)-[:FRIEND]->()
                RETURN u.id AS userId, count(*) AS degree
                ORDER BY degree DESC LIMIT 10
                """);
    }

    @Override
    public void runConcurrentRead(long userId) {
        runPointLookup(userId);
    }

    @Override
    public void runConcurrentWrite(long userId, long targetId, int writeId) {
        graph.query("""
                MATCH (u:User {id: $userId})
                MATCH (v:User {id: $targetId})
                MERGE (u)-[:BENCHMARK_WRITE {id: $writeId}]->(v)
                """, Map.of("userId", userId, "targetId", targetId, "writeId", writeId));
    }
}