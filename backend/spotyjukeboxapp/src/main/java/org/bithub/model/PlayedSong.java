package org.bithub.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "played_songs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayedSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerId;

    private String trackId;

    private LocalDateTime playedAt;

    @PrePersist
    public void onCreate() {
        this.playedAt = LocalDateTime.now();
    }
}
