package org.bithub.controller;

import jakarta.validation.Valid;
import org.bithub.model.TokenPersistingRequest;
import org.bithub.model.UserInfo;
import org.bithub.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    //Registers a new user or updates an existing one using the provided token data.
    @PostMapping("/register")
    public ResponseEntity<?> persist(@Valid @RequestBody TokenPersistingRequest request) {
        UserInfo saved = service.persistOrUpdate(request);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "userId", saved.getSpotifyUserId()
        ));
    }

    //Retrieves user information by Spotify user ID.
    @GetMapping("/get/{userId}")
    public ResponseEntity<?> get(@PathVariable String userId) {
        UserInfo user = service.getById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", "User not found"
            ));
        }
        return ResponseEntity.ok(user);
    }

    //Retrieves user information by email address.
    @GetMapping("/get-by-email/{email}")
    public ResponseEntity<?> getByEmail(@PathVariable String email) {
        UserInfo user = service.getByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", "User not found with email: " + email
            ));
        }
        return ResponseEntity.ok(user);
    }

    //Retrieves a list of all users in the system.
    @GetMapping("/list")
    public ResponseEntity<?> listAll() {
        return ResponseEntity.ok(service.findAll());
    }
}
