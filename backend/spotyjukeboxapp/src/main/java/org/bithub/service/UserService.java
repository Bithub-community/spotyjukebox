package org.bithub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bithub.mapper.UserMapper;
import org.bithub.model.TokenPersistingRequest;
import org.bithub.model.UserInfo;
import org.bithub.persistence.UserInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserInfoRepository userInfoRepository;

    @Transactional
    public UserInfo persist(TokenPersistingRequest request) {
        return persistOrUpdate(request);
    }

    @Transactional
    public UserInfo persistOrUpdate(TokenPersistingRequest request) {
        UserInfo entity = userInfoRepository.findByEmail(request.email())
                .orElseGet(() -> UserInfo.builder()
                        .spotifyUserId(request.userId())
                        .email(request.email())
                        .build());

        // Mapper updates only changed fields (tokens, scopes, etc.)
        UserMapper.updateEntity(entity, request);

        UserInfo saved = userInfoRepository.save(entity);
        log.info("✅ Persisted or updated user: {}", saved.getSpotifyUserId());
        return saved;
    }


    public UserInfo getById(String userId) {
        return userInfoRepository.findBySpotifyUserId(userId).orElse(null);
    }

    public UserInfo getByEmail(String email) {
        return userInfoRepository.findByEmail(email).orElse(null);
    }

    public UserInfo findByRefreshToken(String refreshToken) {
        return userInfoRepository.findByRefreshToken(refreshToken).orElse(null);
    }

    public UserInfo findBySpotifyUserId(String spotifyUserId) {
        return userInfoRepository.findBySpotifyUserId(spotifyUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + spotifyUserId));
    }

    public void save(UserInfo user) {
        userInfoRepository.save(user);
    }

    public List<UserInfo> findAll() {
        return userInfoRepository.findAll();
    }


    public UserInfo get(String userId) {
        return getById(userId);
    }

    public UserInfo getUserById(String userId) {
        return getById(userId);
    }

    public UserInfo getUserBySpotifyId(String userSpotifyId) {
        return getById(userSpotifyId);
    }

    public UserInfo findByEmail(String email) {
        return getByEmail(email);
    }

    public List<UserInfo> findAllActiveJukeboxUsers() {
        log.debug("Returning placeholder list of all users as active Jukebox users");
        return userInfoRepository.findAll();
    }
}
