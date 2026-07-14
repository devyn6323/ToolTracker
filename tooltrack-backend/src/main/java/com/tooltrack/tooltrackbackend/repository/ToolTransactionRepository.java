package com.tooltrack.tooltrackbackend.repository;

import com.tooltrack.tooltrackbackend.model.ToolTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ToolTransactionRepository extends JpaRepository<ToolTransaction, UUID> {
    List<ToolTransaction> findAllByToolIdOrderByCheckedOutAtDesc(UUID toolId);
    Optional<ToolTransaction> findFirstByToolIdAndReturnedAtIsNullOrderByCheckedOutAtDesc(UUID toolId);
    List<ToolTransaction> findAllByUserIdAndReturnedAtIsNullOrderByCheckedOutAtDesc(UUID userId);
    void deleteAllByToolCompanyId(UUID companyId);

    @Query("""
            select tx from ToolTransaction tx
            join fetch tx.tool tool
            join fetch tx.user user
            where tool.company.id = :companyId
            order by coalesce(tx.returnedAt, tx.checkedOutAt) desc
            """)
    List<ToolTransaction> findRecentForCompany(@Param("companyId") UUID companyId, Pageable pageable);
}
