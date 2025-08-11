package com.gotryt.coop.config;

public class JwtConstant {

    public static final String SECRET_KEY = System.getenv("JWT_SECRET_KEY");
    public static final String JWT_HEADER = System.getenv("JWT_HEADER_NAME") != null 
        ? System.getenv("JWT_HEADER_NAME") 
        : "Authorization";

    public static final long JWT_EXPIRATION_MS = getExpiration();

    private static long getExpiration() {
        String value = System.getenv("JWT_EXPIRATION_MS");
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid JWT_EXPIRATION_MS value, falling back to default 3 hours");
            }
        }
        return 3 * 60 * 60 * 1000; // Default to 3 hours
    }
}
