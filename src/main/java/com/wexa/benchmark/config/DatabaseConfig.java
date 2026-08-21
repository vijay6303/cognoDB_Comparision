package com.wexa.benchmark.config;

import io.github.cdimascio.dotenv.Dotenv;

public class DatabaseConfig {

    private final String uri;
    private final String username;
    private final String password;

    public DatabaseConfig() {

        Dotenv dotenv = Dotenv.load();

        this.uri = dotenv.get("COGNODB_URI");
        this.username = dotenv.get("COGNODB_USERNAME");
        this.password = dotenv.get("COGNODB_PASSWORD");

        if (uri == null || username == null || password == null) {
            throw new IllegalStateException(
                    "Missing CognoDB configuration in .env"
            );
        }
    }

    public String getUri() {
        return uri;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}