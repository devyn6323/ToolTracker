package com.tooltrack.tooltrackbackend.dto;

import com.tooltrack.tooltrackbackend.model.ToolCondition;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CheckoutRequest(
        @Size(max = 150) String jobName,
        @Size(max = 150) String location,
        @NotNull @Future Instant expectedReturnAt,
        ToolCondition conditionAtCheckout,
        @Size(max = 2000) String notes) {
}
