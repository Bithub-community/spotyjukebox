package org.bithub.controller;

import lombok.RequiredArgsConstructor;
import org.bithub.model.UserInfo;
import org.bithub.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@RestController
@RequestMapping("/api/auth/spotify")
@RequiredArgsConstructor
public class SpotifyAuthController {

    private final UserService userService;

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;


    //Handles the Spotify callback after user authorization.
    @PostMapping("/callback")
    public ResponseEntity<?> handleSpotifyCallback(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing authorization code"));
        }

        try {
            // Step 1: Exchange authorization code for tokens
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", code);
            params.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, headers);
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                    "https://accounts.spotify.com/api/token", tokenRequest, Map.class
            );

            if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Spotify token exchange failed"));
            }

            Map<String, Object> tokenData = tokenResponse.getBody();
            String accessToken = (String) tokenData.get("access_token");
            String refreshToken = (String) tokenData.get("refresh_token");
            Number expiresIn = (Number) tokenData.get("expires_in");
            String scopeStr = (String) tokenData.get("scope");

            // Step 2: Retrieve user profile from Spotify
            HttpHeaders profileHeaders = new HttpHeaders();
            profileHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> profileRequest = new HttpEntity<>(profileHeaders);

            ResponseEntity<Map> profileResponse = restTemplate.exchange(
                    "https://api.spotify.com/v1/me",
                    HttpMethod.GET,
                    profileRequest,
                    Map.class
            );

            Map<String, Object> profileData = profileResponse.getBody();
            String spotifyUserId = (String) profileData.get("id");
            String displayName = (String) profileData.get("display_name");
            String email = (String) profileData.get("email");

            // Step 3: Save or update user in the local database
            UserInfo user = userService.getById(spotifyUserId);
            if (user == null) {
                user = new UserInfo();
                user.setSpotifyUserId(spotifyUserId);
                user.setCreatedAt(java.time.LocalDateTime.now());
            }

            user.setDisplayName(displayName);
            user.setEmail(email);
            user.setAccessToken(accessToken);
            user.setRefreshToken(refreshToken);
            user.setExpiresIn(expiresIn != null ? expiresIn.longValue() : 3600L);
            user.setScopes(scopeStr != null ? new HashSet<>(Set.of(scopeStr.split(" "))) : new HashSet<>());
            user.setUpdatedAt(java.time.LocalDateTime.now());

            userService.save(user);

            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "userId", spotifyUserId,
                    "accessToken", accessToken
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/login")
    public ResponseEntity<?> redirectToSpotifyAuth() {
        if (clientId == null || redirectUri == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Spotify clientId or redirectUri not configured"));
        }

        String scopes = String.join(" ",
                "user-read-email",
                "user-read-private",
                "playlist-read-private",
                "user-read-playback-state",
                "user-modify-playback-state",
                "playlist-modify-private",
                "playlist-modify-public"
        );

        String authorizeUrl = "https://accounts.spotify.com/authorize" +
                "?client_id=" + clientId +
                "&response_type=code" +
                "&redirect_uri=" + redirectUri +
                "&scope=" + scopes +
                "&show_dialog=true";

        return ResponseEntity.ok(Map.of("authorizeUrl", authorizeUrl));
    }


    //Retrieves stored Spotify user information from the database.
    @GetMapping("/me/{userId}")
    public ResponseEntity<?> getSpotifyUser(@PathVariable String userId) {
        UserInfo user = userService.get(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }

        return ResponseEntity.ok(Map.of(
                "userId", user.getSpotifyUserId(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName(),
                "scopes", user.getScopes(),
                "spotifyLinked", true
        ));
    }
}
