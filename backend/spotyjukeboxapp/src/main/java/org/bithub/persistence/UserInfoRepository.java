package org.bithub.persistence;

import org.bithub.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {

    Optional<UserInfo> findByEmail(String email);

    Optional<UserInfo> findBySpotifyUserId(String spotifyUserId);

    Optional<UserInfo> findByRefreshToken(String refreshToken);

    List<UserInfo> findByJukeboxPlaylistIdIsNotNull();
}
