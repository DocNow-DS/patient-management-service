package com.healthcare.patient.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String type;
    private Instant occurredAt;

    private String id;
    private String username;
    private String email;
    private Set<String> roles;
    private boolean enabled;
}
