package org.bithub.validate;

import org.bithub.model.TokenPersistingRequest;

public class TokenPersistRequestValidator {

    public static void validate(TokenPersistingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        if (isBlank(request.userId())) {
            throw new IllegalArgumentException("Spotify user ID (userId) is required.");
        }

        if (isBlank(request.accessToken())) {
            throw new IllegalArgumentException("Access token (accessToken) is required.");
        }

        if (isBlank(request.refreshToken())) {
            throw new IllegalArgumentException("Refresh token (refreshToken) is required.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
