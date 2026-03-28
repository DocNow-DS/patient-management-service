package com.healthcare.patient.dto;

import com.healthcare.patient.model.User;

/**
 * Safe doctor profile for public search / booking (no password, roles, or clinical patient fields).
 */
public record DoctorPublicSummaryDto(
        String id,
        String username,
        String email,
        String name,
        String specialty,
        String licenseNumber,
        Integer yearsOfExperience,
        String qualifications,
        String department,
        String hospitalName,
        String education,
        String about,
        String profileImageUrl,
        Boolean isVerified,
        Boolean enabled) {

    public static DoctorPublicSummaryDto fromUser(User user) {
        if (user == null) {
            return null;
        }
        return new DoctorPublicSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getSpecialty(),
                user.getLicenseNumber(),
                user.getYearsOfExperience(),
                user.getQualifications(),
                user.getDepartment(),
                user.getHospitalName(),
                user.getEducation(),
                user.getAbout(),
                user.getProfileImageUrl(),
                user.getIsVerified(),
                user.isEnabled());
    }
}
