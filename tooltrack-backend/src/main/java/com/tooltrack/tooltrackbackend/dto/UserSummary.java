package com.tooltrack.tooltrackbackend.dto;

import com.tooltrack.tooltrackbackend.model.AppUser;
import com.tooltrack.tooltrackbackend.model.Role;

import java.util.UUID;

public record UserSummary(UUID id, String name, String email, Role role, boolean active) {
    public static UserSummary from(AppUser user) {
        return new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive());
    }
}
