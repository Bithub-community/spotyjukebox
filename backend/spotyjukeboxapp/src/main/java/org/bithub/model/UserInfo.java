package org.bithub.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "user_info",
        indexes = @Index(name = "ux_user_info_user_id", columnList = "spotify_user_id", unique = true)
)
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spotify_user_id", nullable = false)
    private String spotifyUserId;

    private String email;

    private String displayName;

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expires_in", nullable = false)
    private Long expiresIn;

    @Column(name = "jukebox_playlist_id")
    private String jukeboxPlaylistId;

    @ElementCollection
    @CollectionTable(
            name = "user_info_scopes",
            joinColumns = @JoinColumn(name = "user_info_id")
    )
    @Column(name = "scope")
    private Set<String> scopes;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
