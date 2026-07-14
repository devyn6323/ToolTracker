package com.tooltrack.tooltrackbackend.dto;

import com.tooltrack.tooltrackbackend.model.ToolCondition;
import com.tooltrack.tooltrackbackend.model.ToolStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ToolResponse(
        UUID id,
        String assetNumber,
        String name,
        String category,
        String manufacturer,
        String model,
        String serialNumber,
        LocalDate purchaseDate,
        ToolCondition condition,
        ToolStatus status,
        String currentLocation,
        String qrCodeValue,
        String photoUrl,
        String notes,
        Instant createdAt,
        UserSummary checkedOutTo,
        Instant expectedReturnAt) {
}
