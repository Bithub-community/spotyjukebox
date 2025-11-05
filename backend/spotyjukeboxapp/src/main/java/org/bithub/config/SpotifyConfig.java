package org.bithub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


// Configuration class for Spotify integration.Provides beans required for making external API calls.
@Configuration
public class SpotifyConfig {

    /**
     Creates a {@link RestTemplate} bean used across the application for sending HTTP requests to Spotify or other external services.
     @return a configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
