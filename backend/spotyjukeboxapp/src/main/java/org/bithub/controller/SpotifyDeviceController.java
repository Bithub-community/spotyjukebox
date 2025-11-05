package org.bithub.controller;

import lombok.RequiredArgsConstructor;
import org.bithub.model.SpotifyDevice;
import org.bithub.model.UserInfo;
import org.bithub.service.SpotifyService;
import org.bithub.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spotify")
@CrossOrigin(origins = "http://127.0.0.1:3000")
@RequiredArgsConstructor
public class SpotifyDeviceController {

    private final SpotifyService spotifyService;
    private final UserService userService;

    //Retrieves all available Spotify devices for the given user.
    @GetMapping("/devices/{spotifyUserId}")
    public ResponseEntity<?> getDevices(@PathVariable("spotifyUserId") String userSpotifyId) {
        try {
            UserInfo user = userService.getUserBySpotifyId(userSpotifyId);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            List<SpotifyDevice> devices = spotifyService.getAvailableDevices(user);
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch devices"));
        }
    }

    //Starts playback of a specific playlist on a chosen device.
    @PostMapping("/play")
    public ResponseEntity<?> playPlaylist(@RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            String deviceId = body.get("deviceId");
            String playlistId = body.get("playlistId");

            if (userId == null || deviceId == null || playlistId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing parameters"));
            }

            UserInfo user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // Start playlist playback
            spotifyService.playOnDevice(user, deviceId, playlistId);

            // Link or update the user's active Jukebox playlist
            if (user.getJukeboxPlaylistId() == null || user.getJukeboxPlaylistId().isEmpty()
                    || !user.getJukeboxPlaylistId().equals(playlistId)) {
                user.setJukeboxPlaylistId(playlistId);
                userService.save(user);
            }

            return ResponseEntity.ok(Map.of(
                    "status", "playing",
                    "linkedPlaylist", playlistId
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to play playlist"));
        }
    }

    //Retrieves the currently playing track for the specified Spotify user.
    @GetMapping("/now-playing/{ownerId}")
    public ResponseEntity<?> getNowPlaying(@PathVariable String ownerId) {
        try {
            UserInfo user = userService.getUserBySpotifyId(ownerId);
            if (user == null) return ResponseEntity.notFound().build();

            Map<String, Object> data = spotifyService.getNowPlaying(user);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch currently playing track"));
        }
    }
}
