package com.tooltrack.tooltrackbackend.repository;

import com.tooltrack.tooltrackbackend.model.ToolItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ToolRepository extends JpaRepository<ToolItem, UUID> {
    List<ToolItem> findAllByCompanyIdOrderByName(UUID companyId);
    Optional<ToolItem> findByIdAndCompanyId(UUID id, UUID companyId);
    Optional<ToolItem> findByQrCodeValueAndCompanyId(String qrCodeValue, UUID companyId);
    boolean existsByCompanyIdAndAssetNumberIgnoreCase(UUID companyId, String assetNumber);
    void deleteAllByCompanyId(UUID companyId);
}
