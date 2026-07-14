package com.tooltrack.tooltrackbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(@NotBlank String password) {
}
