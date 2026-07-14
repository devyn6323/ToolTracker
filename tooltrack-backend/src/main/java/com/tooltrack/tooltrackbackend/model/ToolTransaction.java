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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tool_transactions")
@Getter
@Setter
@NoArgsConstructor
public class ToolTransaction {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tool_id", nullable = false)
    private ToolItem tool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType transactionType;

    private String jobName;
    private String location;

    @Enumerated(EnumType.STRING)
    private ToolCondition conditionAtCheckout;

    @Enumerated(EnumType.STRING)
    private ToolCondition conditionAtReturn;

    private Instant checkedOutAt;
    private Instant expectedReturnAt;
    private Instant returnedAt;

    @Column(length = 2000)
    private String notes;
}
