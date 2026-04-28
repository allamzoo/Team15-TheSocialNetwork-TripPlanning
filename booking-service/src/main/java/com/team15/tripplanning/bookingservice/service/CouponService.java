package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.model.Coupon;
import com.team15.tripplanning.bookingservice.repository.CouponRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CouponService {
    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public Coupon create(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    public List<Coupon> findAll() {
        return couponRepository.findAll();
    }

    public Coupon findById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found: " + id));
    }

    public Coupon update(Long id, Coupon coupon) {
        Coupon existing = findById(id);
        if (coupon.getCode() != null) {
            existing.setCode(coupon.getCode());
        }
        if (coupon.getDiscountType() != null) {
            existing.setDiscountType(coupon.getDiscountType());
        }
        if (coupon.getDiscountValue() != null) {
            existing.setDiscountValue(coupon.getDiscountValue());
        }
        if (coupon.getMaxUses() != null) {
            existing.setMaxUses(coupon.getMaxUses());
        }
        if (coupon.getCurrentUses() != null) {
            existing.setCurrentUses(coupon.getCurrentUses());
        }
        if (coupon.getExpiryDate() != null) {
            existing.setExpiryDate(coupon.getExpiryDate());
        }
        if (coupon.getActive() != null) {
            existing.setActive(coupon.getActive());
        }
        if (coupon.getMetadata() != null) {
            existing.setMetadata(coupon.getMetadata());
        }
        return couponRepository.save(existing);
    }

    public void delete(Long id) {
        couponRepository.delete(findById(id));
    }
}
