package com.tooltrack.tooltrackbackend.dto;

import com.tooltrack.tooltrackbackend.model.ToolCondition;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReturnRequest(
        @NotNull ToolCondition conditionAtReturn,
        @Size(max = 150) String location,
        @Size(max = 2000) String notes) {
}
