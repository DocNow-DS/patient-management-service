package com.healthcare.patient.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "prescriptions")
public class Prescription {
    @Id
    private String id;
    private String userId;
    private String doctorId;
    private String medication;
    private String dosage;
    private String instructions;
    private LocalDateTime issuedAt;
}