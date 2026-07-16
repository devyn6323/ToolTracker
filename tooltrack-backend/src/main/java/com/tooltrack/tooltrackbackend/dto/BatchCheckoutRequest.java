package com.tooltrack.tooltrackbackend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BatchCheckoutRequest(
        @NotEmpty @Size(max = 100) List<UUID> toolIds,
        @Size(max = 150) String jobName,
        @Size(max = 150) String location,
        @NotNull @Future Instant expectedReturnAt,
        @Size(max = 2000) String notes) {
}
