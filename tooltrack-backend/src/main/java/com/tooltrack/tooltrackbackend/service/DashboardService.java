package com.tooltrack.tooltrackbackend.service;

import com.tooltrack.tooltrackbackend.dto.ActivityResponse;
import com.tooltrack.tooltrackbackend.dto.DashboardResponse;
import com.tooltrack.tooltrackbackend.dto.UserSummary;
import com.tooltrack.tooltrackbackend.model.ToolStatus;
import com.tooltrack.tooltrackbackend.model.ToolTransaction;
import com.tooltrack.tooltrackbackend.repository.ToolRepository;
import com.tooltrack.tooltrackbackend.repository.ToolTransactionRepository;
import com.tooltrack.tooltrackbackend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final ToolRepository toolRepository;
    private final ToolTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UserPrincipal principal) {
        EnumMap<ToolStatus, Long> counts = new EnumMap<>(ToolStatus.class);
        for (ToolStatus status : ToolStatus.values()) counts.put(status, 0L);
        toolRepository.findAllByCompanyIdOrderByName(principal.companyId()).forEach(tool -> {
            ToolTransaction open = transactionRepository
                    .findFirstByToolIdAndReturnedAtIsNullOrderByCheckedOutAtDesc(tool.getId()).orElse(null);
            ToolStatus effective = open != null && open.getExpectedReturnAt() != null
                    && open.getExpectedReturnAt().isBefore(Instant.now()) ? ToolStatus.OVERDUE : tool.getStatus();
            counts.compute(effective, (ignored, value) -> value + 1);
        });
        return new DashboardResponse(counts, activity(principal, 5));
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> activity(UserPrincipal principal, int limit) {
        return transactionRepository.findRecentForCompany(principal.companyId(), PageRequest.of(0, limit)).stream()
                .map(this::toActivity).toList();
    }

    private ActivityResponse toActivity(ToolTransaction transaction) {
        return new ActivityResponse(transaction.getId(), transaction.getTool().getId(), transaction.getTool().getName(),
                transaction.getTool().getAssetNumber(), UserSummary.from(transaction.getUser()),
                transaction.getTransactionType(), transaction.getJobName(), transaction.getLocation(),
                transaction.getConditionAtCheckout(), transaction.getConditionAtReturn(), transaction.getCheckedOutAt(),
                transaction.getExpectedReturnAt(), transaction.getReturnedAt(),
                transaction.getReturnedAt() == null ? transaction.getCheckedOutAt() : transaction.getReturnedAt(),
                transaction.getNotes());
    }
}
