package com.healthcare.patient.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class JwtUtil {
    public String generateToken(UserDetails userDetails) {
        String payload = userDetails.getUsername() + ":" + System.currentTimeMillis();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}