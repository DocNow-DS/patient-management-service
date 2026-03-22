package com.healthcare.patient.controller;

import com.healthcare.patient.model.Patient;
import com.healthcare.patient.model.MedicalReport;
import com.healthcare.patient.model.Prescription;
import com.healthcare.patient.model.User;
import com.healthcare.patient.repository.PatientRepository;
import com.healthcare.patient.repository.PrescriptionRepository;
import com.healthcare.patient.repository.ReportRepository;
import com.healthcare.patient.repository.UserRepository;
import com.healthcare.patient.service.FileStorageService;
import com.healthcare.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final FileStorageService fileStorageService;
    private final ReportRepository reportRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    // ---- /api/patients/** ---- (public read, used by doctor-service)

    /**
     * Get a patient by their patient-profile ID. Used by doctor-service's
     * isPatientValid.
     */
    @GetMapping("/api/patients/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable String id) {
        return patientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Get all patients (for admin/doctor consumption). */
    @GetMapping("/api/patients")
    public ResponseEntity<List<Patient>> getPatients() {
        return ResponseEntity.ok(patientRepository.findAll());
    }

    // ---- /api/patient/** ---- (authenticated, patient self-service)

    @GetMapping("/api/patient/all")
    public ResponseEntity<List<Patient>> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        System.out.println("Total patients found: " + patients.size());
        return ResponseEntity.ok(patients);
    }

    @GetMapping("/api/patient/profile")
    public ResponseEntity<Patient> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(patientService.getPatientProfile(user.getId()));
    }

    @PutMapping("/api/patient/profile")
    public ResponseEntity<Patient> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Patient patient) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        patient.setUserId(user.getId());
        return ResponseEntity.ok(patientService.createOrUpdateProfile(patient));
    }

    @PostMapping("/api/patient/reports")
    public ResponseEntity<MedicalReport> uploadReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam("description") String description) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Patient patient = patientService.getPatientProfile(user.getId());

        String filePath = fileStorageService.storeFile(file);

        MedicalReport report = MedicalReport.builder()
                .patientId(patient.getId())
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .uploadDate(LocalDateTime.now())
                .description(description)
                .build();

        return ResponseEntity.ok(reportRepository.save(report));
    }

    @GetMapping("/api/patient/reports")
    public ResponseEntity<List<MedicalReport>> getReports(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Patient patient = patientService.getPatientProfile(user.getId());
        return ResponseEntity.ok(reportRepository.findByPatientId(patient.getId()));
    }

    @GetMapping("/api/patient/prescriptions")
    public ResponseEntity<List<Prescription>> getPrescriptions(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Patient patient = patientService.getPatientProfile(user.getId());
        return ResponseEntity.ok(prescriptionRepository.findByPatientId(patient.getId()));
    }
}
