package com.tooltrack.tooltrackbackend.service;

import com.tooltrack.tooltrackbackend.dto.AuthResponse;
import com.tooltrack.tooltrackbackend.dto.DeleteAccountRequest;
import com.tooltrack.tooltrackbackend.dto.LoginRequest;
import com.tooltrack.tooltrackbackend.dto.RegisterRequest;
import com.tooltrack.tooltrackbackend.dto.GoogleAuthRequest;
import com.tooltrack.tooltrackbackend.dto.GoogleAuthResponse;
import com.tooltrack.tooltrackbackend.dto.ForgotPasswordRequest;
import com.tooltrack.tooltrackbackend.dto.ResetPasswordRequest;
import com.tooltrack.tooltrackbackend.dto.ChangePasswordRequest;
import com.tooltrack.tooltrackbackend.dto.TransferOwnershipRequest;
import com.tooltrack.tooltrackbackend.dto.UserSummary;
import com.tooltrack.tooltrackbackend.exception.ApiException;
import com.tooltrack.tooltrackbackend.model.AppUser;
import com.tooltrack.tooltrackbackend.model.Company;
import com.tooltrack.tooltrackbackend.model.Role;
import com.tooltrack.tooltrackbackend.repository.CompanyRepository;
import com.tooltrack.tooltrackbackend.repository.UserRepository;
import com.tooltrack.tooltrackbackend.repository.ToolRepository;
import com.tooltrack.tooltrackbackend.repository.ToolTransactionRepository;
import com.tooltrack.tooltrackbackend.security.UserPrincipal;
import com.tooltrack.tooltrackbackend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration RESET_CODE_LIFETIME = Duration.ofMinutes(15);
    private static final int MAX_RESET_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ToolRepository toolRepository;
    private final ToolTransactionRepository transactionRepository;
    private final PhotoStorageService photoStorageService;
    private final GoogleIdentityService googleIdentityService;
    private final PasswordResetMailService passwordResetMailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with that email already exists");
        }

        Company company = companyRepository.save(new Company(request.companyName().trim()));
        AppUser owner = new AppUser();
        owner.setCompany(company);
        owner.setName(request.name().trim());
        owner.setEmail(email);
        owner.setPasswordHash(passwordEncoder.encode(request.password()));
        owner.setRole(Role.OWNER);
        userRepository.save(owner);
        return response(owner);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(this::invalidCredentials);
        if (!user.isActive()) {
            throw invalidCredentials();
        }

        Instant now = Instant.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed login attempts. Try again later");
        }
        if (user.getLockedUntil() != null) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        if (!user.isPasswordLoginEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                user.setLockedUntil(now.plus(LOGIN_LOCK_DURATION));
            }
            throw invalidCredentials();
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return response(user);
    }

    @Transactional
    public GoogleAuthResponse google(GoogleAuthRequest request) {
        GoogleIdentityService.GoogleIdentity identity = googleIdentityService.verify(request.idToken());
        AppUser user = userRepository.findByGoogleSubject(identity.subject()).orElse(null);
        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(identity.email()).orElse(null);
            if (user != null) {
                if (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(identity.subject())) {
                    throw new ApiException(HttpStatus.CONFLICT, "This email is already linked to another Google account");
                }
                user.setGoogleSubject(identity.subject());
                if (user.isPasswordChangeRequired()) {
                    user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    user.setPasswordLoginEnabled(false);
                    user.setPasswordChangeRequired(false);
                    user.setSessionVersion(user.getSessionVersion() + 1);
                }
            }
        }
        if (user != null) {
            if (!user.isActive()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "This ToolTrack account is inactive");
            }
            return GoogleAuthResponse.authenticated(response(user));
        }
        if (request.companyName() == null || request.companyName().isBlank()) {
            return GoogleAuthResponse.onboarding(identity.email(), identity.name());
        }

        Company company = companyRepository.save(new Company(request.companyName().trim()));
        AppUser owner = new AppUser();
        owner.setCompany(company);
        owner.setName(identity.name());
        owner.setEmail(identity.email());
        owner.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        owner.setPasswordLoginEnabled(false);
        owner.setGoogleSubject(identity.subject());
        owner.setRole(Role.OWNER);
        userRepository.save(owner);
        return GoogleAuthResponse.authenticated(response(owner));
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(AppUser::isActive)
                .filter(AppUser::isPasswordLoginEnabled)
                .orElse(null);
        if (user == null) return;

        String code = String.format("%08d", SECURE_RANDOM.nextInt(100_000_000));
        user.setPasswordResetCodeHash(passwordEncoder.encode(code));
        user.setPasswordResetExpiresAt(Instant.now().plus(RESET_CODE_LIFETIME));
        user.setPasswordResetAttempts(0);
        passwordResetMailService.sendResetCode(user.getEmail(), user.getName(), code);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void resetPassword(ResetPasswordRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(AppUser::isActive)
                .filter(AppUser::isPasswordLoginEnabled)
                .orElseThrow(this::invalidResetCode);
        Instant now = Instant.now();
        if (user.getPasswordResetCodeHash() == null || user.getPasswordResetExpiresAt() == null
                || !user.getPasswordResetExpiresAt().isAfter(now)
                || user.getPasswordResetAttempts() >= MAX_RESET_ATTEMPTS) {
            clearResetCode(user);
            throw invalidResetCode();
        }
        if (!passwordEncoder.matches(request.code(), user.getPasswordResetCodeHash())) {
            user.setPasswordResetAttempts(user.getPasswordResetAttempts() + 1);
            if (user.getPasswordResetAttempts() >= MAX_RESET_ATTEMPTS) clearResetCode(user);
            throw invalidResetCode();
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangeRequired(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setSessionVersion(user.getSessionVersion() + 1);
        clearResetCode(user);
    }

    @Transactional
    public AuthResponse changePassword(ChangePasswordRequest request, UserPrincipal principal) {
        AppUser user = userRepository.findWithCompanyById(principal.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        if (!user.isPasswordLoginEnabled()
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangeRequired(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setSessionVersion(user.getSessionVersion() + 1);
        clearResetCode(user);
        return response(user);
    }

    @Transactional
    public AuthResponse transferOwnership(UUID targetUserId, TransferOwnershipRequest request,
                                          UserPrincipal principal) {
        AppUser owner = userRepository.findWithCompanyById(principal.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        if (owner.getRole() != Role.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the company owner can transfer ownership");
        }
        confirmIdentity(owner, request.password(), request.googleIdToken());
        if (owner.getId().equals(targetUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose another team member");
        }
        AppUser target = userRepository.findByIdAndCompanyId(targetUserId, principal.companyId())
                .filter(AppUser::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team member not found"));

        owner.setRole(Role.MANAGER);
        owner.setSessionVersion(owner.getSessionVersion() + 1);
        target.setRole(Role.OWNER);
        target.setSessionVersion(target.getSessionVersion() + 1);
        return response(owner);
    }

    @Transactional
    public void deleteAccount(DeleteAccountRequest request, UserPrincipal principal) {
        AppUser user = userRepository.findWithCompanyById(principal.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        confirmIdentity(user, request.password(), request.googleIdToken());

        if (user.getRole() == Role.OWNER) {
            UUID companyId = user.getCompany().getId();
            toolRepository.findAllByCompanyIdOrderByName(companyId).stream()
                    .map(tool -> tool.getPhotoUrl())
                    .forEach(photoStorageService::delete);
            transactionRepository.deleteAllByToolCompanyId(companyId);
            toolRepository.deleteAllByCompanyId(companyId);
            userRepository.deleteAllByCompanyId(companyId);
            companyRepository.deleteById(companyId);
            return;
        }

        user.setActive(false);
        user.setName("Deleted user");
        user.setEmail("deleted-" + user.getId() + "@deleted.invalid");
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setPasswordLoginEnabled(false);
        user.setPasswordChangeRequired(false);
        user.setGoogleSubject(null);
    }

    private AuthResponse response(AppUser user) {
        return new AuthResponse(jwtService.createToken(user), "Bearer", user.getCompany().getId(),
                user.getCompany().getName(), UserSummary.from(user), user.isPasswordLoginEnabled(),
                user.isPasswordChangeRequired());
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    private ApiException invalidResetCode() {
        return new ApiException(HttpStatus.BAD_REQUEST, "Reset code is invalid or expired");
    }

    private void clearResetCode(AppUser user) {
        user.setPasswordResetCodeHash(null);
        user.setPasswordResetExpiresAt(null);
        user.setPasswordResetAttempts(0);
    }

    private void confirmIdentity(AppUser user, String password, String googleIdToken) {
        if (user.isPasswordLoginEnabled()) {
            if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Password is incorrect");
            }
            return;
        }
        if (googleIdToken == null || googleIdToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Confirm this action with Google");
        }
        GoogleIdentityService.GoogleIdentity identity = googleIdentityService.verify(googleIdToken);
        if (!identity.subject().equals(user.getGoogleSubject())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google account does not match this ToolTrack account");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
