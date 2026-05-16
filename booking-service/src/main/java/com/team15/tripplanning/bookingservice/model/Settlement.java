package com.team15.tripplanning.bookingservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents one payment settlement per itinerary.
 * The unique index on itinerary_id is the idempotency guard:
 * if the same ItineraryCompletedEvent is delivered twice,
 * the second INSERT violates the constraint and the consumer skips it.
 *
 * Status flow:
 *   SETTLEMENT_PENDING  → ItineraryCompletedEvent consumed
 *   SETTLED             → PaymentCompletedEvent received (POST /settlement/process success)
 *   PAYMENT_FAILED      → PaymentFailedEvent received  (POST /settlement/process failure)
 *   REFUNDED            → itinerary.cancelled consumed + refund processed
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
        SETTLEMENT_PENDING,
        SETTLED,
        PAYMENT_FAILED,
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
    private SettlementStatus status = SettlementStatus.SETTLEMENT_PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public Settlement() {}

    public Settlement(Long itineraryId, Long userId, BigDecimal amount) {
        this.itineraryId = itineraryId;
        this.userId      = userId;
        this.amount      = amount;
        this.status      = SettlementStatus.SETTLEMENT_PENDING;
    }

    // ── Getters / setters ─────────────────────────────────────────────────────

    public Long getId()                        { return id; }
    public Long getItineraryId()               { return itineraryId; }
    public void setItineraryId(Long v)         { this.itineraryId = v; }
    public Long getUserId()                    { return userId; }
    public void setUserId(Long v)              { this.userId = v; }
    public BigDecimal getAmount()              { return amount; }
    public void setAmount(BigDecimal v)        { this.amount = v; }
    public SettlementStatus getStatus()        { return status; }
    public void setStatus(SettlementStatus v)  { this.status = v; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public LocalDateTime getSettledAt()        { return settledAt; }
    public void setSettledAt(LocalDateTime v)  { this.settledAt = v; }
    public String getFailureReason()           { return failureReason; }
    public void setFailureReason(String v)     { this.failureReason = v; }
}
