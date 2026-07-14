package com.tooltrack.tooltrackbackend.service;

import com.tooltrack.tooltrackbackend.dto.AuthResponse;
import com.tooltrack.tooltrackbackend.dto.DeleteAccountRequest;
import com.tooltrack.tooltrackbackend.dto.LoginRequest;
import com.tooltrack.tooltrackbackend.dto.RegisterRequest;
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

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(15);
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ToolRepository toolRepository;
    private final ToolTransactionRepository transactionRepository;
    private final PhotoStorageService photoStorageService;

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

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
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
    public void deleteAccount(DeleteAccountRequest request, UserPrincipal principal) {
        AppUser user = userRepository.findWithCompanyById(principal.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Password is incorrect");
        }

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
    }

    private AuthResponse response(AppUser user) {
        return new AuthResponse(jwtService.createToken(user), "Bearer", user.getCompany().getId(),
                user.getCompany().getName(), UserSummary.from(user));
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
