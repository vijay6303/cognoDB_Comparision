package com.wexa.benchmark.client;

import com.wexa.benchmark.config.DatabaseConfig;

import java.util.*;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

public class CognoDbClient implements GraphDatabaseClient {

    private final Driver driver;

    public CognoDbClient(DatabaseConfig config) {

        this.driver = GraphDatabase.driver(
                config.getUri(),
                AuthTokens.basic(
                        config.getUsername(),
                        config.getPassword()
                )
        );
    }

    @Override
    public void connect() {

        driver.verifyConnectivity();

        System.out.println(
                "Connected to CognoDB successfully!"
        );
    }

    @Override
    public void close() {

        driver.close();
    }

    @Override
    public void execute(String query) {

        driver.executableQuery(query).execute();
    }

    @Override
    public long countNodes() {

        var result = driver.executableQuery(
                "MATCH (n) RETURN count(n) AS count"
        ).execute();

        return result.records()
                .get(0)
                .get("count")
                .asLong();
    }

    public void addRelationships(
            List<Long> sources,
            List<Long> destinations) {

        List<Map<String, Object>> rows = new ArrayList<>();

        for (int i = 0; i < sources.size(); i++) {

            rows.add(
                    Map.of(
                            "source", sources.get(i),
                            "destination", destinations.get(i)
                    )
            );
        }

        driver.executableQuery("""
            UNWIND $rows AS row

            MERGE (a:User {id: row.source})
            MERGE (b:User {id: row.destination})
            MERGE (a)-[:FRIEND]->(b)
            """)
            .withParameters(Map.of("rows", rows))
            .execute();
    }

    @Override
    public long countRelationships() {

        var result = driver.executableQuery(
                "MATCH ()-[r]->() RETURN count(r) AS count"
        ).execute();

        return result.records()
                .get(0)
                .get("count")
                .asLong();
    }
    public void createIndexes() {

        driver.executableQuery("""
            CREATE INDEX user_id_index IF NOT EXISTS
            FOR (u:User)
            ON (u.id)
            """)
            .execute();

        System.out.println("Indexes created.");
    }

    public void runTraversal(long userId, int hops) {

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
                        "Only 1, 2 and 3 hops are supported."
                );
            }

            driver.executableQuery(query)
                    .withParameters(
                            java.util.Map.of(
                                    "userId", userId
                            )
                    )
                    .execute();
        }
        public void runPointLookup(long userId) {

            String query = """
                MATCH (u:User {id: $userId})
                RETURN u.id AS id
                """;

            driver.executableQuery(query)
                    .withParameters(
                            java.util.Map.of(
                                    "userId", userId
                            )
                    )
                    .execute();
        }

        public void runUserCount() {

            String query = """
                MATCH (u:User)
                RETURN count(u) AS totalUsers
                """;

            driver.executableQuery(query)
                    .execute();
        }
        public void runRelationshipCount() {

            String query = """
                MATCH ()-[r:FRIEND]->()
                RETURN count(r) AS totalRelationships
                """;

            driver.executableQuery(query)
                    .execute();
        }
        public void runTopDegrees() {

            String query = """
                MATCH (u:User)-[:FRIEND]->()
                RETURN u.id AS userId, count(*) AS degree
                ORDER BY degree DESC
                LIMIT 10
                """;

            driver.executableQuery(query)
                    .execute();
        }
        public void runConcurrentRead(long userId) {

            String query = """
                MATCH (u:User {id: $userId})
                RETURN u.id AS id
                """;

            driver.executableQuery(query)
                    .withParameters(
                            Map.of("userId", userId)
                    )
                    .execute();
        }


    public void runConcurrentWrite(long userId, long targetId, int writeId) {

            String query = """
                MATCH (u:User {id: $userId})
                MATCH (v:User {id: $targetId})
                MERGE (u)-[:BENCHMARK_WRITE {id: $writeId}]->(v)
                """;

            driver.executableQuery(query)
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

                List<Long> ids = new ArrayList<>();

                /*
                * Low-degree users
                */
                var lowResult = driver.executableQuery("""
                    MATCH (u:User)-[:FRIEND]->()
                    WITH u, count(*) AS degree
                    ORDER BY degree ASC, u.id ASC
                    LIMIT 3
                    RETURN u.id AS id
                    """)
                    .execute();

                lowResult.records().forEach(
                        record -> ids.add(
                                record.get("id").asLong()
                        )
                );

                /*
                * Middle-degree users
                */
                var middleResult = driver.executableQuery("""
                    MATCH (u:User)-[:FRIEND]->()
                    WITH u, count(*) AS degree
                    ORDER BY degree ASC, u.id ASC
                    SKIP 50
                    LIMIT 4
                    RETURN u.id AS id
                    """)
                    .execute();

                middleResult.records().forEach(
                        record -> ids.add(
                                record.get("id").asLong()
                        )
                );

                /*
                * High-degree users
                */
                var highResult = driver.executableQuery("""
                    MATCH (u:User)-[:FRIEND]->()
                    WITH u, count(*) AS degree
                    ORDER BY degree DESC, u.id ASC
                    LIMIT 3
                    RETURN u.id AS id
                    """)
                    .execute();

                highResult.records().forEach(
                        record -> ids.add(
                                record.get("id").asLong()
                        )
                );

                return ids.stream()
                        .distinct()
                        .toList();
            }
}