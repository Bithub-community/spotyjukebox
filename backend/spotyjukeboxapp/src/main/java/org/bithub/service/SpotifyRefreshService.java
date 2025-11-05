package org.bithub.service;

import lombok.RequiredArgsConstructor;
import org.bithub.model.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SpotifyRefreshService {

    private final UserService userService;

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    public UserInfo refreshAccessToken(UserInfo user) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Prepare HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            // Prepare form parameters
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("refresh_token", user.getRefreshToken());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            // Call Spotify token API
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://accounts.spotify.com/api/token",
                    request,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                System.err.println("⚠️ Failed to refresh token. Status: " + response.getStatusCode());
                return null;
            }

            // Extract new token details
            Map<String, Object> data = response.getBody();
            String newAccessToken = (String) data.get("access_token");
            Number expiresIn = (Number) data.get("expires_in");

            // Update user info
            user.setAccessToken(newAccessToken);
            user.setExpiresIn(expiresIn != null ? expiresIn.longValue() : 3600L);
            user.setUpdatedAt(LocalDateTime.now());

            userService.save(user);
            System.out.println("✅ Successfully refreshed token for user: " + user.getSpotifyUserId());

            return user;

        } catch (Exception e) {
            System.err.println("❌ Error refreshing token for user: " + user.getSpotifyUserId());
            e.printStackTrace();
            return null;
        }
    }
}
