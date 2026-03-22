package com.healthcare.patient.service;

import com.healthcare.patient.model.Patient;
import com.healthcare.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    @Transactional
    public Patient getPatientProfile(String userId) {
        return patientRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPatientProfile(userId));
    }

    private Patient createDefaultPatientProfile(String userId) {
        System.out.println("Creating default patient profile for user: " + userId);
        Patient patient = Patient.builder()
                .userId(userId)
                .name("Unknown")
                .age(null)
                .gender("Unknown")
                .phone("")
                .address("")
                .medicalHistory("")
                .build();
        return patientRepository.save(patient);
    }

    @Transactional
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
