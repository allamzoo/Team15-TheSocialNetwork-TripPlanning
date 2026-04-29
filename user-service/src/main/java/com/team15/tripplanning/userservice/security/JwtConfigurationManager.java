package com.team15.tripplanning.userservice.security;

public class JwtConfigurationManager {

    private static volatile JwtConfigurationManager instance;

    private final String secretKey;
    private final long expirationMs;

    private JwtConfigurationManager() {
        this.secretKey = System.getenv("JWT_SECRET") != null
                ? System.getenv("JWT_SECRET")
                : "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciBqd3Q=";
        this.expirationMs = System.getenv("JWT_EXPIRATION_MS") != null
                ? Long.parseLong(System.getenv("JWT_EXPIRATION_MS"))
                : 86400000L;
    }

    public static JwtConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (JwtConfigurationManager.class) {
                if (instance == null) {
                    instance = new JwtConfigurationManager();
                }
            }
        }
        return instance;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
