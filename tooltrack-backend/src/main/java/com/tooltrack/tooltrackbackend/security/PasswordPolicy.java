package com.tooltrack.tooltrackbackend.security;

public final class PasswordPolicy {
    public static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$";
    public static final String MESSAGE =
            "Password must include an uppercase letter, lowercase letter, number, and special character";

    private PasswordPolicy() {
    }
}
