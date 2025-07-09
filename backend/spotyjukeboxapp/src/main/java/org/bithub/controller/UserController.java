package org.bithub.controller;

import org.bithub.model.TokenPersistingRequest;
import org.bithub.persistence.UserInfo;
import org.bithub.service.UserService;
import org.bithub.validate.TokenPersistRequestValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public void persist(@RequestBody TokenPersistingRequest request) {
        TokenPersistRequestValidator.validate(request);
        userService.persist(request);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserInfo> get(@PathVariable("userId") String userId) {
        return ResponseEntity.ok().body(userService.get(userId));
    }
}
