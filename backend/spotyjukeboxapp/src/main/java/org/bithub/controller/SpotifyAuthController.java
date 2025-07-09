package org.bithub.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/callback")
public class SpotifyAuthController {

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${frontend.redirect.url}")
    private String frontendRedirectUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public void handleSpotifyCallback(@RequestParam String code, HttpServletResponse response) throws IOException {
        // Exchange authorization code for access token
        String tokenUrl = "https://accounts.spotify.com/api/token";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", "http://localhost:8080/callback");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> responseEntity = restTemplate.exchange(
            tokenUrl,
            HttpMethod.POST,
            requestEntity,
            Map.class
        );

        Map<String, String> tokenResponse = responseEntity.getBody();
        String accessToken = tokenResponse.getOrDefault("access_token", null);
        String refreshToken = tokenResponse.getOrDefault("refresh_token", null);

        String redirectUrl = frontendRedirectUrl + "?access_token=" + accessToken;
        response.sendRedirect(redirectUrl);
    }
}