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
@Document(collection = "medical_reports")
public class MedicalReport {
    @Id
    private String id;
    private String userId;
    private String fileName;
    private String filePath;
    private LocalDateTime uploadDate;
    private String description;
}