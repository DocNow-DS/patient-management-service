package com.healthcare.patient.service;

import com.healthcare.patient.model.Patient;
import com.healthcare.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public Patient getPatientProfile(String userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));
    }

    public Patient createOrUpdateProfile(Patient patient) {
        Optional<Patient> existingPatient = patientRepository.findByUserId(patient.getUserId());
        if (existingPatient.isPresent()) {
            Patient p = existingPatient.get();
            p.setName(patient.getName());
            p.setAge(patient.getAge());
            p.setGender(patient.getGender());
            p.setPhone(patient.getPhone());
            p.setAddress(patient.getAddress());
            p.setMedicalHistory(patient.getMedicalHistory());
            return patientRepository.save(p);
        }
        return patientRepository.save(patient);
    }
}
