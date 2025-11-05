package org.bithub.model;

import lombok.Data;

@Data
public class SpotifyPlaylist {

    private String id;

    private String name;

    private String imageUrl;

    private int trackCount;

    private String href;
}
