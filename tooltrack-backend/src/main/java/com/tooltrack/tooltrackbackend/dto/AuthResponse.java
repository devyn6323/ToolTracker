package com.tooltrack.tooltrackbackend.dto;

import java.util.UUID;

public record AuthResponse(String token, String tokenType, UUID companyId, String companyName, UserSummary user,
                           boolean passwordLoginEnabled, boolean passwordChangeRequired) {
}
