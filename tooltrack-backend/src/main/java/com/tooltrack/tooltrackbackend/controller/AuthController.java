package com.tooltrack.tooltrackbackend.controller;

import com.tooltrack.tooltrackbackend.dto.AuthResponse;
import com.tooltrack.tooltrackbackend.dto.LoginRequest;
import com.tooltrack.tooltrackbackend.dto.DeleteAccountRequest;
import com.tooltrack.tooltrackbackend.security.UserPrincipal;
import com.tooltrack.tooltrackbackend.dto.RegisterRequest;
import com.tooltrack.tooltrackbackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@Valid @RequestBody DeleteAccountRequest request,
                              @AuthenticationPrincipal UserPrincipal principal) {
        authService.deleteAccount(request, principal);
    }
}
