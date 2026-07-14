package com.tooltrack.tooltrackbackend.security;

import com.tooltrack.tooltrackbackend.model.Role;

import java.util.UUID;

public record UserPrincipal(UUID id, UUID companyId, String email, Role role) {
}
