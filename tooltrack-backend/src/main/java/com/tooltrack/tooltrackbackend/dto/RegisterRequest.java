package com.tooltrack.tooltrackbackend.dto;

import com.tooltrack.tooltrackbackend.security.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 150) String companyName,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) String password) {
}
