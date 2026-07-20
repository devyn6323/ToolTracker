package com.tooltrack.tooltrackbackend.security;

import com.tooltrack.tooltrackbackend.model.AppUser;
import com.tooltrack.tooltrackbackend.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                JwtService.JwtIdentity identity = jwtService.parseIdentity(header.substring(7));
                AppUser user = userRepository.findWithCompanyById(identity.userId()).filter(AppUser::isActive).orElse(null);
                if (user != null && user.getSessionVersion() == identity.sessionVersion()) {
                    if (user.isPasswordChangeRequired()
                            && !(request.getMethod().equals("PUT") && request.getRequestURI().equals("/api/auth/password"))
                            && !(request.getMethod().equals("DELETE") && request.getRequestURI().equals("/api/auth/account"))) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/problem+json");
                        response.getWriter().write("{\"title\":\"Password change required\",\"detail\":\"Replace the temporary password before using ToolTrack\",\"status\":403}");
                        return;
                    }
                    UserPrincipal principal = new UserPrincipal(user.getId(), user.getCompany().getId(), user.getEmail(), user.getRole());
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
