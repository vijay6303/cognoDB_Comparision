package com.wexa.benchmark.client;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Neo4jClient implements GraphDatabaseClient {

    private final Driver driver;

    public Neo4jClient(
        String uri,
        String username,
        String password) {

        this.driver = GraphDatabase.driver(
                uri,
                AuthTokens.basic(
                        username,
                        password
                )
        );
    }

    @Override
    public void connect() {

        driver.verifyConnectivity();

        System.out.println(
                "Connected to Neo4j."
        );
    }

    @Override
    public void close() {

        driver.close();
    }

    @Override
    public void execute(String query) {

        driver.executableQuery(query)
                .execute();
    }

    @Override
    public long countNodes() {

        var result =
                driver.executableQuery(
                        "MATCH (n) RETURN count(n) AS count"
                ).execute();

        return result.records()
                .get(0)
                .get("count")
                .asLong();
    }

    @Override
    public long countRelationships() {

        var result =
                driver.executableQuery(
                        "MATCH ()-[r]->() RETURN count(r) AS count"
                ).execute();

        return result.records()
                .get(0)
                .get("count")
                .asLong();
    }

    @Override
    public void addRelationships(
            List<Long> sources,
            List<Long> destinations) {

        List<Map<String, Object>> rows =
                new ArrayList<>();

        for (int i = 0;
             i < sources.size();
             i++) {

            rows.add(
                    Map.of(
                            "source",
                            sources.get(i),
                            "destination",
                            destinations.get(i)
                    )
            );
        }

        driver.executableQuery("""
            UNWIND $rows AS row
            MERGE (a:User {id: row.source})
            MERGE (b:User {id: row.destination})
            MERGE (a)-[:FRIEND]->(b)
            """)
            .withParameters(
                    Map.of("rows", rows)
            )
            .execute();
    }

    @Override
    public void createIndexes() {

        driver.executableQuery("""
            CREATE INDEX user_id_index IF NOT EXISTS
            FOR (u:User)
            ON (u.id)
            """)
            .execute();
    }

    @Override
    public void runTraversal(
            long userId,
            int hops) {

        String query;

        if (hops == 1) {

            query = """
                MATCH (u:User {id: $userId})
                    -[:FRIEND]->(friend)
                RETURN count(friend) AS result
                """;

        } else if (hops == 2) {

            query = """
                MATCH (u:User {id: $userId})
                    -[:FRIEND]->()
                    -[:FRIEND]->(friend)
                RETURN count(friend) AS result
                """;

        } else if (hops == 3) {

            query = """
                MATCH (u:User {id: $userId})
                    -[:FRIEND]->()
                    -[:FRIEND]->()
                    -[:FRIEND]->(friend)
                RETURN count(friend) AS result
                """;

        } else {

            throw new IllegalArgumentException(
                    "Only 1, 2 and 3 hops supported."
            );
        }

        driver.executableQuery(query)
                .withParameters(
                        Map.of("userId", userId)
                )
                .execute();
    }

    @Override
    public void runPointLookup(long userId) {

        driver.executableQuery("""
            MATCH (u:User {id: $userId})
            RETURN u.id AS id
            """)
            .withParameters(
                    Map.of("userId", userId)
            )
            .execute();
    }

    @Override
    public void runUserCount() {

        driver.executableQuery("""
            MATCH (u:User)
            RETURN count(u) AS totalUsers
            """)
            .execute();
    }

    @Override
    public void runRelationshipCount() {

        driver.executableQuery("""
            MATCH ()-[r:FRIEND]->()
            RETURN count(r) AS totalRelationships
            """)
            .execute();
    }

    @Override
    public void runTopDegrees() {

        driver.executableQuery("""
            MATCH (u:User)-[:FRIEND]->()
            RETURN u.id AS userId,
                   count(*) AS degree
            ORDER BY degree DESC
            LIMIT 10
            """)
            .execute();
    }

    @Override
    public void runConcurrentRead(
            long userId) {

        runPointLookup(userId);
    }

    @Override
    public void runConcurrentWrite(
            long userId,
            long targetId,
            int writeId) {

        driver.executableQuery("""
            MATCH (u:User {id: $userId})
            MATCH (v:User {id: $targetId})
            MERGE (u)-[:BENCHMARK_WRITE {id: $writeId}]->(v)
            """)
            .withParameters(
                    Map.of(
                            "userId", userId,
                            "targetId", targetId,
                            "writeId", writeId
                    )
            )
            .execute();
    }

    public List<Long> getRepresentativeUserIds() {

        var result = driver.executableQuery("""
            MATCH (u:User)-[:FRIEND]->()
            WITH u, count(*) AS degree
            ORDER BY degree ASC, u.id ASC
            LIMIT 10
            RETURN u.id AS id
            """)
            .execute();

        return result.records()
                .stream()
                .map(
                        r -> r.get("id").asLong()
                )
                .toList();
    }
}