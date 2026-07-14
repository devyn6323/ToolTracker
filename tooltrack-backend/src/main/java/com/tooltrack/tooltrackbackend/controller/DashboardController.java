package com.tooltrack.tooltrackbackend.controller;

import com.tooltrack.tooltrackbackend.dto.ActivityResponse;
import com.tooltrack.tooltrackbackend.dto.DashboardResponse;
import com.tooltrack.tooltrackbackend.security.UserPrincipal;
import com.tooltrack.tooltrackbackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.dashboard(principal);
    }

    @GetMapping("/activity")
    public List<ActivityResponse> activity(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.activity(principal, 50);
    }
}
