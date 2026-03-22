package com.healthcare.patient.controller;

import com.healthcare.patient.service.AuthService;
import com.healthcare.patient.repository.UserRepository;
import com.healthcare.patient.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        System.out.println("Total users found: " + users.size());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthService.RegisterRequest request) {
        AuthService.AuthResult result = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse("Bearer", result.getToken(), result.getUser()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthService.LoginRequest request) {
        AuthService.AuthResult result = authService.login(request);
        return ResponseEntity.ok(new AuthResponse("Bearer", result.getToken(), result.getUser()));
    }

    // Requires Bearer token
    @PutMapping("/me")
    public ResponseEntity<User> updateMyCredentials(Principal principal,
                                                    @RequestBody AuthService.UpdateCredentialsRequest request) {
        User updated = authService.updateCredentials(principal.getName(), request);
        return ResponseEntity.ok(updated);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthResponse {
        private String tokenType;
        private String token;
        private User user;
    }
}
