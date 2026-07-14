package com.tooltrack.tooltrackbackend.dto;

import com.tooltrack.tooltrackbackend.model.ToolCondition;
import com.tooltrack.tooltrackbackend.model.ToolStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ToolRequest(
        @NotBlank @Size(max = 80) String assetNumber,
        @NotBlank @Size(max = 150) String name,
        String category,
        String manufacturer,
        String model,
        String serialNumber,
        LocalDate purchaseDate,
        @NotNull ToolCondition condition,
        ToolStatus status,
        String currentLocation,
        String photoUrl,
        @Size(max = 2000) String notes) {
}
