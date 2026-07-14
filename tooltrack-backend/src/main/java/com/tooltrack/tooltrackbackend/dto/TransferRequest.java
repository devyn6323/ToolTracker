package com.tooltrack.tooltrackbackend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID targetUserId,
        @Size(max = 150) String location,
        @Future Instant expectedReturnAt,
        @Size(max = 2000) String notes) {
}
