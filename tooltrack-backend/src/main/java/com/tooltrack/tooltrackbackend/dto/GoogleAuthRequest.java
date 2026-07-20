package com.tooltrack.tooltrackbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleAuthRequest(
        @NotBlank @Size(max = 4096) String idToken,
        @Size(min = 2, max = 150) String companyName) {
}
