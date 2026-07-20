package com.tooltrack.tooltrackbackend.dto;

import jakarta.validation.constraints.Size;

public record TransferOwnershipRequest(
        @Size(max = 72) String password,
        @Size(max = 8192) String googleIdToken) {
}
