package org.bithub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bithub.model.SpotifyDevice;
import org.bithub.model.SpotifyPlaylist;
import org.bithub.model.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyService {

    private final SpotifyRefreshService spotifyRefreshService;
    private final VoteService voteService;
    private final RestTemplate restTemplate;
    private final UserService userService;

    @Value("${spotify.api.url}")
    private String spotifyApiUrl;

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;



    // Refreshes the Spotify access token for the given user.
    public String refreshAccessToken(UserInfo user) {
        String url = "https://accounts.spotify.com/api/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", user.getRefreshToken());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String newToken = (String) response.getBody().get("access_token");
                Number expiresIn = (Number) response.getBody().get("expires_in");

                user.setAccessToken(newToken);
                user.setExpiresIn(expiresIn.longValue());
                userService.save(user);

                log.info("✅ Refreshed Spotify token for user {}", user.getSpotifyUserId());
                return newToken;
            }

            log.error("❌ Spotify token refresh failed for user {}", user.getSpotifyUserId());
            return null;

        } catch (Exception e) {
            log.error("❌ Error refreshing token for user {}", user.getSpotifyUserId(), e);
            return null;
        }
    }


    // --------------------------------------------------------------------
    // DEVICES
    // --------------------------------------------------------------------

    // Fetches all available playback devices for a user.
    public List<SpotifyDevice> getAvailableDevices(UserInfo user) {
        String url = spotifyApiUrl + "/me/player/devices";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user.getAccessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            List<Map<String, Object>> devicesData = (List<Map<String, Object>>) response.getBody().get("devices");

            if (devicesData == null) return Collections.emptyList();

            return devicesData.stream()
                    .map(d -> new SpotifyDevice(
                            (String) d.get("id"),
                            (String) d.get("name"),
                            (String) d.get("type"),
                            Boolean.TRUE.equals(d.get("is_active"))
                    ))
                    .collect(Collectors.toList());

        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Access token expired while fetching devices. Refreshing...");
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            return getAvailableDevices(refreshed);

        } catch (Exception e) {
            log.error("❌ Error fetching Spotify devices for user {}", user.getSpotifyUserId(), e);
            return Collections.emptyList();
        }
    }

    //Gets the currently active device for the user.
    private String getActiveDeviceId(UserInfo user) {
        try {
            List<SpotifyDevice> devices = getAvailableDevices(user);

            return devices.stream()
                    .filter(SpotifyDevice::isActive)
                    .map(SpotifyDevice::getId)
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            log.error("❌ Failed to get active device ID for user {}", user.getSpotifyUserId(), e);
            return null;
        }
    }


    //Starts playing the given playlist on the specified device.
    public void playOnDevice(UserInfo user, String deviceId, String playlistId) {
        String url = spotifyApiUrl + "/me/player/play?device_id=" + deviceId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("context_uri", "spotify:playlist:" + playlistId);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("🎵 Playing playlist {} on device {}", playlistId, deviceId);

        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Access token expired during playback. Refreshing...");
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            playOnDevice(refreshed, deviceId, playlistId);

        } catch (Exception e) {
            log.error("❌ Failed to start playback on device {}", deviceId, e);
        }
    }


    // Retrieves the currently playing track.
    public Map<String, Object> getNowPlaying(UserInfo user) {
        String url = spotifyApiUrl + "/me/player/currently-playing";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user.getAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // ✅ NEW: Cooldown ekleme
                try {
                    boolean isPlaying = Boolean.TRUE.equals(body.get("is_playing"));
                    if (isPlaying && body.containsKey("item")) {
                        Map<String, Object> item = (Map<String, Object>) body.get("item");
                        String currentTrackId = (String) item.get("id");

                        if (currentTrackId != null && !currentTrackId.isBlank()) {
                            voteService.addPlayedSong(user.getSpotifyUserId(), currentTrackId);
                            log.debug("🕐 Added to cooldown: {} for user {}", currentTrackId, user.getSpotifyUserId());
                        }
                    }
                } catch (Exception cooldownError) {
                    log.warn("⚠️ Failed to update cooldown for user {}", user.getSpotifyUserId(), cooldownError);
                }

                return body;
            }
            return Map.of("is_playing", false);

        } catch (HttpClientErrorException.Unauthorized e) {
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            return getNowPlaying(refreshed);

        } catch (Exception e) {
            log.error("❌ Error fetching 'Now Playing' for user {}", user.getSpotifyUserId(), e);
            return Map.of("error", "Failed to fetch currently playing track");
        }
    }



    // Retrieves and sorts the Spotify queue based on votes and cooldowns.
    public Map<String, Object> getQueue(UserInfo user) {
        String url = spotifyApiUrl + "/me/player/queue";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user.getAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) return Map.of();

            List<Map<String, Object>> queue = (List<Map<String, Object>>) body.get("queue");
            if (queue == null) queue = new ArrayList<>();

            Map<String, Long> votes = voteService.getActiveVotes(user.getSpotifyUserId());
            List<String> cooldown = voteService.getCooldownTracks(user.getSpotifyUserId());

            // add votes
            queue.forEach(track -> {
                String id = (String) track.get("id");
                track.put("votes", votes.getOrDefault(id, 0L));
            });

            // sort by votes descending
            queue.sort(Comparator.comparingLong((Map<String, Object> t) ->
                    (long) t.getOrDefault("votes", 0L)).reversed());

            // cooldown to bottom
            queue.sort((a, b) -> {
                boolean aCool = cooldown.contains(a.get("id"));
                boolean bCool = cooldown.contains(b.get("id"));
                return Boolean.compare(aCool, bCool);
            });

            body.put("queue", queue);
            return body;

        } catch (HttpClientErrorException.Unauthorized e) {
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            return getQueue(refreshed);

        } catch (Exception e) {
            log.error("❌ Failed to fetch Spotify queue for user {}", user.getSpotifyUserId(), e);
            return Map.of("error", "Queue unavailable");
        }
    }


    // Creates a new private "Jukebox" playlist for the user.
    public String createJukeboxPlaylist(UserInfo user) {
        String url = spotifyApiUrl + "/users/" + user.getSpotifyUserId() + "/playlists";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "name", "🎵 Jukebox - " + System.currentTimeMillis(),
                "description", "Dynamic voting-based playlist",
                "public", false
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            String playlistId = (String) response.getBody().get("id");

            user.setJukeboxPlaylistId(playlistId);
            userService.save(user);

            log.info("✅ Created Jukebox playlist for user {}", user.getSpotifyUserId());
            return playlistId;

        } catch (HttpClientErrorException.Unauthorized e) {
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            return createJukeboxPlaylist(refreshed);

        } catch (Exception e) {
            log.error("❌ Failed to create Jukebox playlist for {}", user.getSpotifyUserId(), e);
            throw new RuntimeException("Playlist creation failed");
        }
    }

    // Fetches the tracks from a given Spotify playlist.
    public List<Map<String, Object>> getPlaylistTracks(UserInfo user, String playlistId) {
        String url = spotifyApiUrl + "/playlists/" + playlistId + "/tracks";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user.getAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");

            if (items == null) return Collections.emptyList();
            return items.stream()
                    .map(i -> (Map<String, Object>) i.get("track"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (HttpClientErrorException.Unauthorized e) {
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            return getPlaylistTracks(refreshed, playlistId);
        } catch (Exception e) {
            log.error("❌ Failed to fetch playlist tracks for {}", playlistId, e);
            return Collections.emptyList();
        }
    }

    // Legacy method name - delegates to getUserPlaylists(). Kept for backwards compatibility with existing code.
    @Deprecated
    public List<SpotifyPlaylist> getUserPlaylists() {
        try {
            // TODO: Implement actual Spotify API call
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch playlists", e);
        }
    }


    //Updates the user's Jukebox playlist order based on current votes. Keeps the currently playing song at the top.
    public void updateJukeboxPlaylist(UserInfo user) {
        try {
            String playlistId = user.getJukeboxPlaylistId();
            if (playlistId == null || playlistId.isBlank()) {
                log.info("⏸️ No active Jukebox for user {}", user.getSpotifyUserId());
                return;
            }

            // 1️⃣ Fetch currently playing track
            Map<String, Object> nowPlaying = getNowPlaying(user);
            String currentUri = null, currentId = null;
            if (nowPlaying != null && nowPlaying.containsKey("item")) {
                Map<String, Object> item = (Map<String, Object>) nowPlaying.get("item");
                currentId = (String) item.get("id");
                currentUri = (String) item.get("uri");
            }

            // 2️⃣ Fetch playlist tracks
            List<Map<String, Object>> currentTracks = getPlaylistTracks(user, playlistId);
            if (currentTracks.isEmpty()) {
                log.warn("⚠️ Jukebox playlist is empty for {}", user.getSpotifyUserId());
                return;
            }

            // 3️⃣ Sort playlist by votes
            List<String> orderedUris = sortPlaylistByVotes(user, currentTracks);
            if (orderedUris.isEmpty()) {
                log.warn("⚠️ No valid tracks to reorder for {}", user.getSpotifyUserId());
                return;
            }

            // 4️⃣ Keep currently playing track first
            if (currentUri != null) {
                orderedUris.remove(currentUri);
                orderedUris.add(0, currentUri);
            }

            // 5️⃣ Replace playlist content
            replacePlaylistTracks(user, playlistId, orderedUris);
            log.info("✅ Updated Jukebox playlist order for {}", user.getSpotifyUserId());


        } catch (Exception e) {
            log.error("❌ Failed to update Jukebox playlist for {}", user.getSpotifyUserId(), e);
        }
    }

    // Replaces the playlist content with a new list of tracks, keeping Spotify's batch limit (100 tracks/request).
    private void replacePlaylistTracks(UserInfo user, String playlistId, List<String> uris) {
        String url = spotifyApiUrl + "/playlists/" + playlistId + "/tracks";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        final int batchSize = 100;

        for (int i = 0; i < uris.size(); i += batchSize) {
            List<String> batch = uris.subList(i, Math.min(i + batchSize, uris.size()));
            Map<String, Object> body = Map.of("uris", batch);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            try {
                if (i == 0)
                    restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
                else
                    restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            } catch (HttpClientErrorException.Unauthorized e) {
                UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
                replacePlaylistTracks(refreshed, playlistId, uris);
                return;
            } catch (Exception e) {
                log.warn("⚠️ Failed to replace tracks in playlist {}", playlistId, e);
            }
        }
    }

    // Sorts playlist tracks based on active votes and cooldown rules.
    private List<String> sortPlaylistByVotes(UserInfo user, List<Map<String, Object>> tracks) {
        Map<String, Long> votes = voteService.getActiveVotes(user.getSpotifyUserId());
        List<String> cooldownTracks = voteService.getCooldownTracks(user.getSpotifyUserId());

        // Normalize helper for track ID comparison
        Function<String, String> normalize = ref -> {
            if (ref == null) return null;
            return ref.startsWith("spotify:track:") ? ref.substring("spotify:track:".length()) : ref;
        };

        List<Map<String, Object>> voted = new ArrayList<>();
        List<Map<String, Object>> unvoted = new ArrayList<>();
        List<Map<String, Object>> cooldown = new ArrayList<>();

        for (Map<String, Object> track : tracks) {
            String trackId = (String) track.get("id");
            String uri = (String) track.get("uri");

            if (trackId == null || uri == null) continue;

            String normalizedId = normalize.apply(trackId);
            String normalizedUri = normalize.apply(uri);

            boolean isCooldown = cooldownTracks.stream()
                    .anyMatch(t -> t.equals(normalizedId) || t.equals(normalizedUri));

            long count = votes.getOrDefault(normalizedId,
                    votes.getOrDefault(normalizedUri, 0L));

            if (isCooldown) {
                cooldown.add(track);
            } else if (count > 0) {
                track.put("votes", count);
                voted.add(track);
            } else {
                unvoted.add(track);
            }
        }

        voted.sort(Comparator.comparingLong(
                (Map<String, Object> t) -> (long) t.get("votes")).reversed());

        List<String> orderedUris = new ArrayList<>();
        voted.forEach(t -> orderedUris.add((String) t.get("uri")));
        unvoted.forEach(t -> orderedUris.add((String) t.get("uri")));
        cooldown.forEach(t -> orderedUris.add((String) t.get("uri")));

        log.info("📊 Sorted playlist → {} voted | {} unvoted | {} cooldown",
                voted.size(), unvoted.size(), cooldown.size());

        return orderedUris;
    }


    // Plays the user's Jukebox playlist on the selected device. Automatically creates one if it doesn't exist.
    public void playJukeboxPlaylist(UserInfo user, String deviceId) {
        String playlistId = user.getJukeboxPlaylistId();

        if (playlistId == null || playlistId.isBlank()) {
            log.info("⚠️ No existing Jukebox playlist for user {}. Creating one...", user.getSpotifyUserId());
            playlistId = createJukeboxPlaylist(user);
        }

        playOnDevice(user, deviceId, playlistId);
        log.info("🎵 Started Jukebox playlist on device {}", deviceId);
    }

    // Adds the top-voted track (not in cooldown) to the user's Spotify queue.
    public void reorderQueueByVotes(UserInfo user) {
        try {
            String playlistId = user.getJukeboxPlaylistId();
            if (playlistId == null || playlistId.isBlank()) {
                log.info("⏸️ No active Jukebox for {}", user.getSpotifyUserId());
                return;
            }

            Map<String, Object> nowPlaying = getNowPlaying(user);
            String currentTrackId = nowPlaying.containsKey("item")
                    ? (String) ((Map<?, ?>) nowPlaying.get("item")).get("id")
                    : null;

            List<Map<String, Object>> playlistTracks = getPlaylistTracks(user, playlistId);
            if (playlistTracks.isEmpty()) return;

            Map<String, Long> votes = voteService.getActiveVotes(user.getSpotifyUserId());
            List<String> cooldown = voteService.getCooldownTracks(user.getSpotifyUserId());

            // Filter out current + cooldown tracks
            List<Map<String, Object>> votable = playlistTracks.stream()
                    .filter(track -> {
                        String id = (String) track.get("id");
                        return id != null && !id.equals(currentTrackId) && !cooldown.contains(id);
                    })
                    .toList();

            if (votable.isEmpty()) return;

            // Find top voted
            Map<String, Object> topTrack = votable.stream()
                    .max(Comparator.comparingLong(t ->
                            votes.getOrDefault((String) t.get("id"), 0L)))
                    .orElse(null);

            if (topTrack == null) return;

            String topTrackId = (String) topTrack.get("id");
            long topVotes = votes.getOrDefault(topTrackId, 0L);

            if (topVotes > 0) {
                addToQueue(user, topTrackId);
                log.info("✅ Added top-voted track '{}' ({} votes) to queue",
                        topTrack.get("name"), topVotes);
            } else {
                log.info("⚠️ No votes available to reorder queue.");
            }

        } catch (Exception e) {
            log.error("❌ Failed to reorder queue for {}", user.getSpotifyUserId(), e);
        }
    }

    // Adds a track to the user's Spotify playback queue.
    private void addToQueue(UserInfo user, String trackId) {
        String url = spotifyApiUrl + "/me/player/queue?uri=spotify:track:" + trackId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user.getAccessToken());

        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            addToQueue(refreshed, trackId);
        } catch (Exception e) {
            log.warn("⚠️ Failed to add track {} to queue for {}", trackId, user.getSpotifyUserId());
        }
    }

    // Returns the list of upcoming tracks from the user's playlist enriched with live vote counts — for client display.
    public List<Map<String, Object>> getUpcomingTracksWithVotes(UserInfo user) {
        try {
            String playlistId = user.getJukeboxPlaylistId();
            if (playlistId == null) return Collections.emptyList();

            Map<String, Object> nowPlaying = getNowPlaying(user);
            String currentTrackId = nowPlaying.containsKey("item")
                    ? (String) ((Map<?, ?>) nowPlaying.get("item")).get("id")
                    : null;

            List<Map<String, Object>> tracks = getPlaylistTracks(user, playlistId);

            List<Map<String, Object>> upNext = tracks.stream()
                    .filter(t -> !Objects.equals(t.get("id"), currentTrackId))
                    .collect(Collectors.toList());

            Map<String, Long> votes = voteService.getActiveVotes(user.getSpotifyUserId());

            // ✅ Cooldown bilgisi ekle
            upNext.forEach(t -> {
                String trackId = (String) t.get("id");
                t.put("votes", votes.getOrDefault(trackId, 0L));

                // Cooldown kontrolü
                int cooldownRemaining = voteService.getCooldownRemaining(
                        user.getSpotifyUserId(),
                        trackId
                );
                t.put("cooldownRemaining", cooldownRemaining);
                t.put("inCooldown", cooldownRemaining > 0);
            });

            upNext.sort(Comparator.comparingLong(
                    (Map<String, Object> t) -> (long) t.getOrDefault("votes", 0L)).reversed());

            return upNext;

        } catch (Exception e) {
            log.error("❌ Failed to fetch upcoming tracks with votes", e);
            return Collections.emptyList();
        }
    }
}