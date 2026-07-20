package com.tooltrack.tooltrackbackend.repository;

import com.tooltrack.tooltrackbackend.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    Optional<AppUser> findByGoogleSubject(String googleSubject);
    @EntityGraph(attributePaths = "company")
    Optional<AppUser> findWithCompanyById(UUID id);
    boolean existsByEmailIgnoreCase(String email);
    Optional<AppUser> findByIdAndCompanyId(UUID id, UUID companyId);
    List<AppUser> findAllByCompanyIdOrderByName(UUID companyId);
    void deleteAllByCompanyId(UUID companyId);
}
