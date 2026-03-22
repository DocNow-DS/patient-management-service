package com.healthcare.patient.service;

import com.healthcare.patient.model.Role;
import com.healthcare.patient.model.User;
import com.healthcare.patient.repository.UserRepository;
import com.healthcare.patient.security.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResult register(RegisterRequest request) {
        System.out.println("Registering user: " + request.getUsername());
        
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        // map role: default to PATIENT if not provided, support case-insensitive match
        Role mappedRole;
        String roleStr = request.getRole();
        if (roleStr == null || roleStr.isBlank()) {
            mappedRole = Role.PATIENT;
        } else {
            try {
                mappedRole = Role.valueOf(roleStr.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid role. Allowed values: PATIENT, DOCTOR, ADMIN");
            }
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .roles(Set.of(mappedRole))
                .enabled(true)
                .build();

        System.out.println("Saving user: " + user);
        User savedUser = userRepository.save(user);
        System.out.println("Saved user with ID: " + savedUser.getId());
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails);
        return new AuthResult(token, savedUser);
    }

    public AuthResult login(LoginRequest request) {
        String loginUsername = resolveUsernameForLogin(request);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginUsername,
                        request.getPassword()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(loginUsername);
        String token = jwtUtil.generateToken(userDetails);
        User user = userRepository.findByUsername(loginUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        return new AuthResult(token, user);
    }

    @Transactional
    public User updateCredentials(String currentUsername, UpdateCredentialsRequest request) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim();
            Optional<User> existing = userRepository.findByEmail(newEmail);
            if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            }
            user.setEmail(newEmail);
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userRepository.save(user);
    }

    private String resolveUsernameForLogin(LoginRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            User user = userRepository.findByEmail(request.getEmail().trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password"));
            return user.getUsername();
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username or email is required");
        }
        return request.getUsername().trim();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthResult {
        private String token;
        private User user;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String role;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginRequest {
        private String username;
        private String email;
        private String password;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateCredentialsRequest {
        private String email;
        private String password;
    }
}
