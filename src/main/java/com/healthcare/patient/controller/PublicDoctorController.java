package com.healthcare.patient.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthcare.patient.dto.DoctorPublicSummaryDto;
import com.healthcare.patient.model.Role;
import com.healthcare.patient.model.User;
import com.healthcare.patient.repository.UserRepository;

@RestController
@RequestMapping("/api/public/doctors")
public class PublicDoctorController {

    private final UserRepository userRepository;

    public PublicDoctorController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * List enabled doctors. Optional {@code specialty} filters by case-insensitive substring match
     * on the doctor's {@code specialty} field (e.g. "Cardiology", "cardio").
     * When {@code specialty} is provided and no doctor matches, returns 404 with a structured body.
     */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String specialty) {
        List<User> doctors = userRepository.findByRolesContaining(Role.DOCTOR).stream()
                .filter(User::isEnabled)
                .toList();

        boolean specialtyRequested = specialty != null && !specialty.isBlank();

        if (specialtyRequested) {
            String needle = specialty.trim().toLowerCase();
            doctors = doctors.stream()
                    .filter(u -> u.getSpecialty() != null
                            && u.getSpecialty().toLowerCase().contains(needle))
                    .toList();
        }

        List<DoctorPublicSummaryDto> result = doctors.stream().map(DoctorPublicSummaryDto::fromUser).toList();

        if (specialtyRequested && result.isEmpty()) {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("error", "NO_DOCTORS_FOR_SPECIALTY");
            body.put(
                    "message",
                    "No doctors found for specialty \"" + specialty.trim()
                            + "\". Try another keyword or browse All specialties.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        if (!specialtyRequested && result.isEmpty()) {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("error", "NO_DOCTORS_AVAILABLE");
            body.put("message", "No doctors are available to book right now.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        return ResponseEntity.ok(result);
    }
}
