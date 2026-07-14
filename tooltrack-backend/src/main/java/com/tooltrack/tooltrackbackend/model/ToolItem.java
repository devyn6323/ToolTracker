package com.tooltrack.tooltrackbackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tools", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tool_company_asset", columnNames = {"company_id", "asset_number"}),
        @UniqueConstraint(name = "uk_tool_qr", columnNames = "qr_code_value")
})
@Getter
@Setter
@NoArgsConstructor
public class ToolItem {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "asset_number", nullable = false, length = 80)
    private String assetNumber;

    @Column(nullable = false, length = 150)
    private String name;

    private String category;
    private String manufacturer;
    private String model;
    private String serialNumber;
    private LocalDate purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ToolCondition condition = ToolCondition.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ToolStatus status = ToolStatus.AVAILABLE;

    private String currentLocation;

    @Column(name = "qr_code_value", nullable = false, updatable = false)
    private String qrCodeValue = UUID.randomUUID().toString();

    private String photoUrl;

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Version
    private long version;
}
