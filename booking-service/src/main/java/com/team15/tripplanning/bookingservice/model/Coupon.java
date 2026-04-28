package com.team15.tripplanning.bookingservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import javax.persistence.Entity;

@Entity
@jakarta.persistence.Entity
@Table(name = "coupons")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Coupon {
    public enum DiscountType {
        PERCENTAGE,
        FIXED;

        @JsonCreator
        public static DiscountType fromValue(String value) {
            if (value == null) {
                return null;
            }
            return DiscountType.valueOf(value.trim().toUpperCase());
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonAlias({"coupon_code"})
    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @JsonAlias({"discount_type"})
    @Column
    private DiscountType discountType;

    @JsonAlias({"discount_value"})
    @Column(nullable = false)
    private Double discountValue;

    @JsonAlias({"max_uses"})
    @Column(nullable = false)
    private Integer maxUses;

    @JsonAlias({"current_uses"})
    @Column(nullable = false)
    private Integer currentUses = 0;

    @JsonAlias({"expiry_date"})
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @JsonAlias({"is_active"})
    @Column(nullable = false)
    private Boolean active = true;

    @JsonAlias({"coupon_metadata"})
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata = new HashMap<>();

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingCoupon> bookingCoupons = new ArrayList<>();

    public Coupon() {
    }

    @PrePersist
    public void prePersist() {
        if (discountType == null) {
            discountType = DiscountType.PERCENTAGE;
        }
        if (currentUses == null) {
            currentUses = 0;
        }
        if (active == null) {
            active = true;
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }
    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
    public Integer getCurrentUses() { return currentUses; }
    public void setCurrentUses(Integer currentUses) { this.currentUses = currentUses; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    @JsonSetter("expiryDate")
    @JsonAlias({"expiry_date"})
    public void setExpiryDateString(String expiryDate) {
        if (expiryDate == null || expiryDate.isBlank()) {
            this.expiryDate = null;
            return;
        }
        try {
            this.expiryDate = LocalDateTime.parse(expiryDate, DateTimeFormatter.ISO_DATE_TIME);
            return;
        } catch (Exception ignored) {
            // fall through to date-only parsing
        }
        this.expiryDate = LocalDate.parse(expiryDate, DateTimeFormatter.ISO_DATE).atStartOfDay();
    }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    public List<BookingCoupon> getBookingCoupons() { return bookingCoupons; }
    public void setBookingCoupons(List<BookingCoupon> bookingCoupons) {
        this.bookingCoupons = bookingCoupons != null ? bookingCoupons : new ArrayList<>();
    }
}


