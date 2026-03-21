package com.healthcare.patient.repository;

import com.healthcare.patient.model.MedicalReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReportRepository extends MongoRepository<MedicalReport, String> {
    List<MedicalReport> findByPatientId(String patientId);
}