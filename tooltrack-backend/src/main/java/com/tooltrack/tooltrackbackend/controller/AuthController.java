package com.tooltrack.tooltrackbackend.controller;

import com.tooltrack.tooltrackbackend.dto.AuthResponse;
import com.tooltrack.tooltrackbackend.dto.LoginRequest;
import com.tooltrack.tooltrackbackend.dto.DeleteAccountRequest;
import com.tooltrack.tooltrackbackend.dto.GoogleAuthRequest;
import com.tooltrack.tooltrackbackend.dto.GoogleAuthResponse;
import com.tooltrack.tooltrackbackend.dto.ForgotPasswordRequest;
import com.tooltrack.tooltrackbackend.dto.ResetPasswordRequest;
import com.tooltrack.tooltrackbackend.dto.ChangePasswordRequest;
import com.tooltrack.tooltrackbackend.dto.TransferOwnershipRequest;
import com.tooltrack.tooltrackbackend.security.UserPrincipal;
import com.tooltrack.tooltrackbackend.dto.RegisterRequest;
import com.tooltrack.tooltrackbackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;
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

    @PostMapping("/google")
    public GoogleAuthResponse google(@Valid @RequestBody GoogleAuthRequest request) {
        return authService.google(request);
    }

    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }

    @PutMapping("/password")
    public AuthResponse changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return authService.changePassword(request, principal);
    }

    @PutMapping("/ownership/{targetUserId}")
    @PreAuthorize("hasRole('OWNER')")
    public AuthResponse transferOwnership(@PathVariable UUID targetUserId,
                                          @Valid @RequestBody TransferOwnershipRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return authService.transferOwnership(targetUserId, request, principal);
    }

    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@Valid @RequestBody DeleteAccountRequest request,
                              @AuthenticationPrincipal UserPrincipal principal) {
        authService.deleteAccount(request, principal);
    }
}
