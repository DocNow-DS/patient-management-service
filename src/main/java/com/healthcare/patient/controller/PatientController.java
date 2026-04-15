package com.healthcare.patient.controller;

import com.healthcare.patient.model.MedicalReport;
import com.healthcare.patient.model.User;
import com.healthcare.patient.model.Prescription;
import com.healthcare.patient.repository.PrescriptionRepository;
import com.healthcare.patient.repository.ReportRepository;
import com.healthcare.patient.repository.UserRepository;
import com.healthcare.patient.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ReportRepository reportRepository;
    private final PrescriptionRepository prescriptionRepository;

    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllPatients() {
        List<User> users = userRepository.findAll();
        System.out.println("Total patients found: " + users.size());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody User userUpdates) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (userUpdates.getName() != null) user.setName(userUpdates.getName());
        if (userUpdates.getAge() != null) user.setAge(userUpdates.getAge());
        if (userUpdates.getGender() != null) user.setGender(userUpdates.getGender());
        if (userUpdates.getPhone() != null) user.setPhone(userUpdates.getPhone());
        if (userUpdates.getAddress() != null) user.setAddress(userUpdates.getAddress());
        if (userUpdates.getMedicalHistory() != null) user.setMedicalHistory(userUpdates.getMedicalHistory());
        
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PostMapping("/reports")
    public ResponseEntity<MedicalReport> uploadReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String filePath = fileStorageService.storeFile(file);

        MedicalReport report = MedicalReport.builder()
                .userId(user.getId())
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .uploadDate(LocalDateTime.now())
                .description(description == null ? "" : description)
                .build();

        return ResponseEntity.ok(reportRepository.save(report));
    }

    @GetMapping("/reports")
    public ResponseEntity<List<MedicalReport>> getReports(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(normalizeReportUrls(findReportsForUser(user)));
    }

    @GetMapping("/{id}/reports")
    public ResponseEntity<List<MedicalReport>> getReportsByPatientId(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        return ResponseEntity.ok(normalizeReportUrls(findReportsForUser(user)));
    }

    private List<MedicalReport> findReportsForUser(User user) {
        List<String> candidateIds = new ArrayList<>();
        if (user.getId() != null && !user.getId().isBlank()) candidateIds.add(user.getId().trim());
        if (user.getUsername() != null && !user.getUsername().isBlank()) candidateIds.add(user.getUsername().trim());
        if (user.getEmail() != null && !user.getEmail().isBlank()) candidateIds.add(user.getEmail().trim());

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        List<MedicalReport> rawReports = reportRepository.findByUserIdIn(candidateIds);

        // De-duplicate in case a report matches more than one legacy key.
        Map<String, MedicalReport> byId = new LinkedHashMap<>();
        for (MedicalReport report : rawReports) {
            if (report == null) continue;
            String key = report.getId() == null ? Integer.toString(System.identityHashCode(report)) : report.getId();
            byId.putIfAbsent(key, report);
        }

        return new ArrayList<>(byId.values());
    }

    private List<MedicalReport> normalizeReportUrls(List<MedicalReport> reports) {
        for (MedicalReport report : reports) {
            report.setFilePath(normalizeReportUrl(report.getFilePath()));
        }
        return reports;
    }

    private String normalizeReportUrl(String rawUrl) {
        if (rawUrl == null) return null;

        String normalized = rawUrl.trim();
        if (normalized.isEmpty()) return normalized;

        normalized = normalized.replace(".storage.storage.supabase.co", ".storage.supabase.co");
        normalized = normalized.replaceAll("(?<!\\.storage)\\.supabase\\.co(?=/storage/v1/)", ".storage.supabase.co");
        normalized = normalized.replace("/storage/v1/s3/object/public/", "/storage/v1/object/public/");
        normalized = normalized.replace("/storage/v1/s3/object/", "/storage/v1/object/");
        return fileStorageService.resolveAccessUrl(normalized);
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<List<Prescription>> getPrescriptions(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(prescriptionRepository.findByUserId(user.getId()));
    }

    @GetMapping("/{id}/prescriptions")
    public ResponseEntity<List<Prescription>> getPrescriptionsByPatientId(@PathVariable String id) {
        userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        return ResponseEntity.ok(prescriptionRepository.findByUserId(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getPatientById(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        return ResponseEntity.ok(user);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getPatientByUsername(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return ResponseEntity.ok(user);
    }
}
