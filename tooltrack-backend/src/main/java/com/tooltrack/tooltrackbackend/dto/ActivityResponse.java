package com.tooltrack.tooltrackbackend.dto;

import com.tooltrack.tooltrackbackend.model.ToolCondition;
import com.tooltrack.tooltrackbackend.model.TransactionType;

import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        UUID toolId,
        String toolName,
        String assetNumber,
        UserSummary user,
        TransactionType transactionType,
        String jobName,
        String location,
        ToolCondition conditionAtCheckout,
        ToolCondition conditionAtReturn,
        Instant checkedOutAt,
        Instant expectedReturnAt,
        Instant returnedAt,
        Instant occurredAt,
        String notes) {
}
