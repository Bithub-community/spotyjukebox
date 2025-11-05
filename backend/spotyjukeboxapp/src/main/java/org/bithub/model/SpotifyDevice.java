package org.bithub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpotifyDevice {

    private String id;

    private String name;

    private String type;

    private boolean isActive;
}
