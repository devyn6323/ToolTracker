package com.tooltrack.tooltrackbackend.controller;

import com.tooltrack.tooltrackbackend.dto.EmployeeRequest;
import com.tooltrack.tooltrackbackend.dto.UserSummary;
import com.tooltrack.tooltrackbackend.security.UserPrincipal;
import com.tooltrack.tooltrackbackend.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @GetMapping
    public List<UserSummary> list(@AuthenticationPrincipal UserPrincipal principal) {
        return employeeService.list(principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
    public UserSummary create(@Valid @RequestBody EmployeeRequest request,
                              @AuthenticationPrincipal UserPrincipal principal) {
        return employeeService.create(request, principal);
    }
}
