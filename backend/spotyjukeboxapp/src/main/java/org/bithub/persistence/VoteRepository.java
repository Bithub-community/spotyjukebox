package org.bithub.persistence;

import jakarta.transaction.Transactional;
import org.bithub.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Query("SELECT v.trackId, COUNT(v) FROM Vote v " +
            "WHERE v.ownerId = :ownerId AND v.createdAt > :since " +
            "GROUP BY v.trackId ORDER BY COUNT(v) DESC")
    List<Object[]> findRecentVoteCounts(@Param("ownerId") String ownerId,
                                        @Param("since") LocalDateTime since);

    @Modifying
    @Transactional
    @Query("DELETE FROM Vote v WHERE v.ownerId = :ownerId AND v.trackId = :trackId")
    void deleteVotesForTrack(@Param("ownerId") String ownerId, @Param("trackId") String trackId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Vote v WHERE v.createdAt < :threshold")
    int deleteOldVotes(@Param("threshold") LocalDateTime threshold);

    List<Vote> findByOwnerIdAndTrackId(String ownerId, String trackId);
}
