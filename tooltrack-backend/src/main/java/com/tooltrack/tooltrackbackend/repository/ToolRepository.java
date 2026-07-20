package com.tooltrack.tooltrackbackend.repository;

import com.tooltrack.tooltrackbackend.model.ToolItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ToolRepository extends JpaRepository<ToolItem, UUID> {
    List<ToolItem> findAllByCompanyIdOrderByName(UUID companyId);
    Optional<ToolItem> findByIdAndCompanyId(UUID id, UUID companyId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tool from ToolItem tool where tool.id = :id and tool.company.id = :companyId")
    Optional<ToolItem> findForUpdate(@Param("id") UUID id, @Param("companyId") UUID companyId);
    Optional<ToolItem> findByQrCodeValueAndCompanyId(String qrCodeValue, UUID companyId);
    boolean existsByCompanyIdAndAssetNumberIgnoreCase(UUID companyId, String assetNumber);
    void deleteAllByCompanyId(UUID companyId);
}
