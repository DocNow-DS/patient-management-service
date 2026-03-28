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
import java.util.List;

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
            @RequestParam("description") String description) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String filePath = fileStorageService.storeFile(file);

        MedicalReport report = MedicalReport.builder()
                .userId(user.getId())
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .uploadDate(LocalDateTime.now())
                .description(description)
                .build();

        return ResponseEntity.ok(reportRepository.save(report));
    }

    @GetMapping("/reports")
    public ResponseEntity<List<MedicalReport>> getReports(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(reportRepository.findByUserId(user.getId()));
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<List<Prescription>> getPrescriptions(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(prescriptionRepository.findByUserId(user.getId()));
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
