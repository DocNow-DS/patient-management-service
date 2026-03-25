package com.healthcare.patient.controller;

import com.healthcare.patient.service.AuthService;
import com.healthcare.patient.repository.UserRepository;
import com.healthcare.patient.model.User;
import com.healthcare.patient.security.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final JwtUtil jwtUtil;

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

    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Generate a new token for the validated user
        String token = jwtUtil.generateToken(userDetails);
        
        return ResponseEntity.ok(new AuthResponse("Bearer", token, user));
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
