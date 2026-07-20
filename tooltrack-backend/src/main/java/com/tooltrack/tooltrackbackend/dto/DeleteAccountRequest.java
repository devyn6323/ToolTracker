package com.tooltrack.tooltrackbackend.dto;

import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(@Size(max = 72) String password, @Size(max = 4096) String googleIdToken) {
}
