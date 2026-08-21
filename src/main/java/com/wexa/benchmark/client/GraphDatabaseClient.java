package com.wexa.benchmark.client;

import java.util.List;

public interface GraphDatabaseClient {

    void connect();

    void close();

    void execute(String query);

    long countNodes();

    long countRelationships();

    void addRelationships(
            List<Long> sources,
            List<Long> destinations
    );

    void createIndexes();

        List<Long> getRepresentativeUserIds();

    void runTraversal(
            long userId,
            int hops
    );

    void runPointLookup(
            long userId
    );

    void runUserCount();

    void runRelationshipCount();

    void runTopDegrees();

    void runConcurrentRead(
            long userId
    );

    void runConcurrentWrite(
            long userId,
            long targetId,
            int writeId
    );
}