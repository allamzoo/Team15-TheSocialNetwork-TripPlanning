package com.team15.tripplanning.bookingservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@jakarta.persistence.Entity
@Entity
@Table(name = "bookings")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Booking {
    public enum BookingType {
        ACCOMMODATION,
        TRANSPORT,
        ACTIVITY
    }

    public enum BookingStatus {
        PENDING,
        CONFIRMED,
        FAILED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonAlias({"itinerary_id"})
    @Column(name = "itin_id", nullable = false)
    private Long itineraryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itin_id", referencedColumnName = "id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_bookings_itineraries"))
    private ItineraryRef itinerary;

    @JsonAlias({"user_id"})
    @Column(nullable = false)
    private Long userId;

    @JsonAlias({"booking_amount"})
    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @JsonAlias({"booking_type"})
    @Column(nullable = false)
    private BookingType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @JsonAlias({"booking_status"})
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @JsonAlias({"booking_details"})
    @Column(columnDefinition = "jsonb default '{}'::jsonb")
    private Map<String, Object> bookingDetails = new HashMap<>();

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingCoupon> bookingCoupons = new ArrayList<>();

    public Booking() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BookingStatus.PENDING;
        }
        if (bookingDetails == null) {
            bookingDetails = new HashMap<>();
        }
    }

    @PreUpdate
    public void preUpdate() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public BookingType getType() { return type; }
    public void setType(BookingType type) { this.type = type; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public Map<String, Object> getBookingDetails() { return bookingDetails; }
    public void setBookingDetails(Map<String, Object> bookingDetails) {
        this.bookingDetails = bookingDetails != null ? bookingDetails : new HashMap<>();
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<BookingCoupon> getBookingCoupons() { return bookingCoupons; }
    public void setBookingCoupons(List<BookingCoupon> bookingCoupons) {
        this.bookingCoupons = bookingCoupons != null ? bookingCoupons : new ArrayList<>();
    }
}


