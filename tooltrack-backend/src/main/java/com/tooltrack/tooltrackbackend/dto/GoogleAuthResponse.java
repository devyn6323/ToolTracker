package com.tooltrack.tooltrackbackend.dto;

public record GoogleAuthResponse(boolean onboardingRequired, AuthResponse session, String email, String name) {
    public static GoogleAuthResponse onboarding(String email, String name) {
        return new GoogleAuthResponse(true, null, email, name);
    }

    public static GoogleAuthResponse authenticated(AuthResponse session) {
        return new GoogleAuthResponse(false, session, session.user().email(), session.user().name());
    }
}
