package com.healthcare.patient.repository;

import com.healthcare.patient.model.Prescription;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PrescriptionRepository extends MongoRepository<Prescription, String> {
    List<Prescription> findByUserId(String userId);
}
