package org.bithub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bithub.model.UserInfo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

// Scheduled service responsible for periodically updating Jukebox playlists based on active user votes.
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueSchedulerService {

    private final UserService userService;
    private final SpotifyService spotifyService;

    @Scheduled(fixedRate = 10000) // 10 seconds (testing)
    // @Scheduled(fixedRate = 120000) // 2 minutes (production)
    public void updateJukeboxPlaylists() {
        try {
            List<UserInfo> activeUsers = userService.findAllActiveJukeboxUsers();

            if (activeUsers.isEmpty()) {
                System.out.println("⏸️ No active jukebox sessions.");
                return;
            }

            System.out.println("🔄 Updating jukebox playlists for " + activeUsers.size() + " users...");

            for (UserInfo user : activeUsers) {
                try {
                    spotifyService.updateJukeboxPlaylist(user);
                } catch (Exception e) {
                    System.err.println("❌ Failed to update playlist for user: " + user.getSpotifyUserId());
                    e.printStackTrace();
                }
            }


        } catch (Exception e) {
            System.err.println("❌ Jukebox scheduler encountered an unexpected error:");
            e.printStackTrace();
        }
    }
}
