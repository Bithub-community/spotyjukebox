package org.bithub.mapper;

import org.bithub.model.TokenPersistingRequest;
import org.bithub.model.UserInfo;

import java.util.Set;

public class UserMapper {

    public static void updateEntity(UserInfo entity, TokenPersistingRequest request) {
        entity.setSpotifyUserId(request.userId());
        entity.setAccessToken(request.accessToken());
        entity.setRefreshToken(request.refreshToken());
        entity.setEmail(request.email());
        entity.setDisplayName(request.displayName());

        // Prevent NullPointerException if scopes are null
        Set<String> scopes = request.scopes();
        entity.setScopes(scopes != null ? scopes : Set.of());
    }
}
