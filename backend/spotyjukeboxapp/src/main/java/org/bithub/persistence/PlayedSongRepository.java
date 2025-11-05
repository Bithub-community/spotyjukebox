package org.bithub.persistence;

import jakarta.transaction.Transactional;
import org.bithub.model.PlayedSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayedSongRepository extends JpaRepository<PlayedSong, Long> {

    @Query("SELECT p.trackId FROM PlayedSong p WHERE p.ownerId = :ownerId ORDER BY p.playedAt DESC LIMIT 3")
    List<String> findLast3Songs(@Param("ownerId") String ownerId);

    @Transactional
    @Modifying
    @Query("DELETE FROM PlayedSong p WHERE p.ownerId = :ownerId AND p.trackId = :trackId")
    void deleteByOwnerIdAndTrackId(@Param("ownerId") String ownerId, @Param("trackId") String trackId);
}
