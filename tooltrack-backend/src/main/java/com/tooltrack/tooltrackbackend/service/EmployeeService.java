package com.tooltrack.tooltrackbackend.service;

import com.tooltrack.tooltrackbackend.dto.EmployeeRequest;
import com.tooltrack.tooltrackbackend.dto.UserSummary;
import com.tooltrack.tooltrackbackend.exception.ApiException;
import com.tooltrack.tooltrackbackend.model.AppUser;
import com.tooltrack.tooltrackbackend.model.Role;
import com.tooltrack.tooltrackbackend.repository.CompanyRepository;
import com.tooltrack.tooltrackbackend.repository.UserRepository;
import com.tooltrack.tooltrackbackend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserSummary> list(UserPrincipal principal) {
        return userRepository.findAllByCompanyIdOrderByName(principal.companyId()).stream()
                .map(UserSummary::from).toList();
    }

    @Transactional
    public UserSummary create(EmployeeRequest request, UserPrincipal principal) {
        if (request.role() == Role.OWNER || request.role() == Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New users can be managers or employees");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with that email already exists");
        }
        AppUser employee = new AppUser();
        employee.setCompany(companyRepository.getReferenceById(principal.companyId()));
        employee.setName(request.name().trim());
        employee.setEmail(email);
        employee.setPasswordHash(passwordEncoder.encode(request.password()));
        employee.setPasswordChangeRequired(true);
        employee.setRole(request.role());
        return UserSummary.from(userRepository.save(employee));
    }
}
