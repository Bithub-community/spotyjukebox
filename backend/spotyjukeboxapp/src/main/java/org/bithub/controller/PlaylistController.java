package org.bithub.controller;

import lombok.RequiredArgsConstructor;
import org.bithub.model.UserInfo;
import org.bithub.service.SpotifyService;
import org.bithub.service.UserService;
import org.bithub.service.SpotifyRefreshService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class PlaylistController {

    private final UserService userService;
    private final SpotifyRefreshService spotifyRefreshService;
    private final SpotifyService spotifyService;


    //Retrieves a user's Spotify playlists. If the access token has expired, it attempts to refresh it and retry the request.
    @GetMapping("/playlists/{userId}")
    public ResponseEntity<?> getUserPlaylists(@PathVariable String userId) {
        UserInfo user = userService.get(userId);
        if (user == null || user.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User or access token not found"));
        }

        try {
            return fetchPlaylists(user);

        } catch (HttpClientErrorException.Unauthorized e) {
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            if (refreshed == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Failed to refresh token"));
            }

            try {
                return fetchPlaylists(refreshed);
            } catch (Exception retryEx) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Failed to fetch after token refresh"));
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Sends a GET request to the Spotify API to retrieve a user's playlists.
    private ResponseEntity<?> fetchPlaylists(UserInfo user) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user.getAccessToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.spotify.com/v1/me/playlists?limit=20&offset=0",
                HttpMethod.GET,
                entity,
                Map.class
        );

        return ResponseEntity.ok(response.getBody());
    }

    // Retrieves the current playback queue for the specified Spotify user.
    @GetMapping("/queue/{ownerId}")
    public ResponseEntity<?> getQueue(@PathVariable String ownerId) {
        try {
            UserInfo user = userService.getUserBySpotifyId(ownerId);
            if (user == null) return ResponseEntity.notFound().build();

            Map<String, Object> queueData = spotifyService.getQueue(user);
            return ResponseEntity.ok(queueData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch Spotify queue"));
        }
    }

     //Retrieves upcoming tracks in the user's Spotify playlist along with vote data.
    @GetMapping("/upcoming-tracks/{ownerId}")
    public ResponseEntity<?> getUpcomingTracks(@PathVariable String ownerId) {
        try {
            UserInfo user = userService.findBySpotifyUserId(ownerId);
            List<Map<String, Object>> tracks = spotifyService.getUpcomingTracksWithVotes(user);
            return ResponseEntity.ok(Map.of("queue", tracks));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
