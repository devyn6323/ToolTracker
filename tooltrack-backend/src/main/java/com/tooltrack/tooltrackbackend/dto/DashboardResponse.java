package com.tooltrack.tooltrackbackend.dto;

import com.tooltrack.tooltrackbackend.model.ToolStatus;

import java.util.List;
import java.util.Map;

public record DashboardResponse(Map<ToolStatus, Long> counts, List<ActivityResponse> recentActivity) {
}
