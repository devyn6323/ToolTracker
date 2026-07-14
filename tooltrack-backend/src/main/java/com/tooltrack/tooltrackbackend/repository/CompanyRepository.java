package com.tooltrack.tooltrackbackend.repository;

import com.tooltrack.tooltrackbackend.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
}
