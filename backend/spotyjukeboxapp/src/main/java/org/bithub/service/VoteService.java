package org.bithub.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bithub.model.PlayedSong;
import org.bithub.model.TrackVote;
import org.bithub.model.Vote;
import org.bithub.persistence.PlayedSongRepository;
import org.bithub.persistence.VoteRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final PlayedSongRepository playedSongRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Scheduled(fixedRate = 60_000) // 1 minute (test) → change to 5 min in production
    @Transactional
    public void cleanupOldVotesScheduled() {
        cleanupOldVotes();
    }

    @Transactional
    public void cleanupOldVotes() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        int deleted = voteRepository.deleteOldVotes(threshold);
        if (deleted > 0) {
            log.info("🧹 Cleaned {} expired votes (before {}).", deleted, threshold);
        }
    }


    @Transactional
    public Vote addVote(String ownerId, String trackId, String clientId) {
        cleanupOldVotes();

        boolean alreadyVoted = voteRepository.findByOwnerIdAndTrackId(ownerId, trackId)
                .stream()
                .anyMatch(v -> v.getClientId().equals(clientId));

        if (alreadyVoted) {
            throw new RuntimeException("You have already voted for this song.");
        }

        Vote vote = Vote.builder()
                .ownerId(ownerId)
                .trackId(trackId)
                .clientId(clientId)
                .createdAt(LocalDateTime.now())
                .build();

        Vote saved = voteRepository.save(vote);
        log.info("🗳️ Added new vote → owner={} track={} client={}", ownerId, trackId, clientId);
        return saved;
    }


    @Transactional
    public void resetVotesForPlayedTrack(String ownerId, String trackId) {
        log.info("Resetting votes for track: {}", trackId);

        voteRepository.deleteVotesForTrack(ownerId, trackId);
        long remaining = voteRepository.findByOwnerIdAndTrackId(ownerId, trackId).size();

        if (remaining == 0) {
            log.info("✅ Votes successfully reset for {}", trackId);
        } else {
            log.warn("⚠️ Votes not fully deleted for {} ({} remaining)", trackId, remaining);
        }

        playedSongRepository.save(PlayedSong.builder()
                .ownerId(ownerId)
                .trackId(trackId)
                .playedAt(LocalDateTime.now())
                .build());
    }

    public Map<String, Long> getActiveVotes(String ownerId) {
        cleanupOldVotes();

        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        Map<String, Long> result = new HashMap<>();

        for (Object[] row : voteRepository.findRecentVoteCounts(ownerId, cutoff)) {
            result.put((String) row[0], (Long) row[1]);
        }

        return result;
    }

    public List<String> getCooldownTracks(String ownerId) {
        return playedSongRepository.findLast3Songs(ownerId);
    }

    public List<TrackVote> getRankedTracks(String ownerId) {
        Map<String, Long> votes = getActiveVotes(ownerId);

        return votes.entrySet().stream()
                .map(e -> new TrackVote(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.votes(), a.votes()))
                .toList();
    }

    @Transactional
    public void addPlayedSong(String userId, String trackId) {
        try {
            // don't add if already in cooldown
            List<String> recent = getCooldownTracks(userId);
            if (recent.contains(trackId)) return;

            playedSongRepository.save(
                    PlayedSong.builder()
                            .ownerId(userId)
                            .trackId(trackId)
                            .build()
            );

            if (recent.size() >= 3) {
                String oldestTrackId = recent.get(0);
                playedSongRepository.deleteByOwnerIdAndTrackId(userId, oldestTrackId);
                log.debug("🗑️ Removed oldest cooldown track {}", oldestTrackId);
            }

            log.debug("🎶 Added {} to cooldown for {}", trackId, userId);
        } catch (Exception e) {
            log.warn("⚠️ Failed to add played song {} for {}", trackId, userId, e);
        }
    }

    public int getCooldownRemaining(String ownerId, String trackId) {
        try {
            // take last 3 songs
            List<String> recentTracks = getRecentlyPlayedTrackIds(ownerId, 3);

            // is track in cooldown?
            int position = recentTracks.indexOf(trackId);

            if (position == -1) {
                return 0;
            }

            int remaining = 3 - position;
            return Math.max(remaining, 0);

        } catch (Exception e) {
            log.warn("⚠️ Cooldown check failed for track {}: {}", trackId, e.getMessage());
            return 0;
        }
    }

    private List<String> getRecentlyPlayedTrackIds(String ownerId, int limit) {
        String historyKey = "jukebox:history:" + ownerId;

        try {
            List<String> rawHistory = redisTemplate.opsForList()
                    .range(historyKey, 0, limit - 1);


            if (rawHistory == null || rawHistory.isEmpty()) {
                return Collections.emptyList();
            }

            return rawHistory.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Failed to fetch play history for {}", ownerId, e);
            return Collections.emptyList();
        }
    }

    public void addToPlayHistory(String ownerId, String trackId) {
        String historyKey = "jukebox:history:" + ownerId;

        try {
            List<String> last = redisTemplate.opsForList().range(historyKey, 0, 0);

            if (last != null && !last.isEmpty() && last.get(0).equals(trackId)) {
                log.debug("⏭️ Skipped duplicate track in history: {}", trackId);
                return;
            }

            redisTemplate.opsForList().leftPush(historyKey, trackId);

            redisTemplate.opsForList().trim(historyKey, 0, 9);

            redisTemplate.expire(historyKey, 1, TimeUnit.HOURS);

            log.info("✅ Added {} to play history for {}", trackId, ownerId);

        } catch (Exception e) {
            log.error("❌ Failed to add to play history", e);
        }
    }

    public boolean isInCooldown(String ownerId, String trackId) {
        return getCooldownRemaining(ownerId, trackId) > 0;
    }
}