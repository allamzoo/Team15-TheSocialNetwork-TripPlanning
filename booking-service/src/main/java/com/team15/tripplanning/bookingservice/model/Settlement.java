package com.team15.tripplanning.bookingservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents one payment settlement per itinerary.
 * The unique index on itinerary_id is the idempotency guard:
 * if the same ItineraryCompletedEvent is delivered twice,
 * the second INSERT violates the constraint and the consumer skips it.
 *
 * Status flow:
 *   PENDING     → ItineraryCompletedEvent consumed, payment.initiated published
 *   PROCESSING  → POST /settlement/process received, optimistic lock acquired
 *   COMPLETED   → payment.completed published (success path)
 *   FAILED      → payment.failed published (rejection path)
 *   REFUNDED    → itinerary.cancelled consumed + refund processed
 */
@Entity
@Table(
        name = "settlements",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_settlements_itinerary_id",
                columnNames = "itinerary_id"
        )
)
public class Settlement {

    public enum SettlementStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REFUNDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "itinerary_id", nullable = false, unique = true)
    private Long itineraryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public Settlement() {}

    public Settlement(Long itineraryId, Long userId, BigDecimal amount) {
        this.itineraryId = itineraryId;
        this.userId      = userId;
        this.amount      = amount;
        this.status      = SettlementStatus.PENDING;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null)    status    = SettlementStatus.PENDING;
    }

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public Long getItineraryId()                { return itineraryId; }
    public void setItineraryId(Long v)          { this.itineraryId = v; }

    public Long getUserId()                     { return userId; }
    public void setUserId(Long v)               { this.userId = v; }

    public BigDecimal getAmount()               { return amount; }
    public void setAmount(BigDecimal v)         { this.amount = v; }

    public SettlementStatus getStatus()         { return status; }
    public void setStatus(SettlementStatus v)   { this.status = v; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }

    public LocalDateTime getSettledAt()         { return settledAt; }
    public void setSettledAt(LocalDateTime v)   { this.settledAt = v; }

    public String getFailureReason()            { return failureReason; }
    public void setFailureReason(String v)      { this.failureReason = v; }
}