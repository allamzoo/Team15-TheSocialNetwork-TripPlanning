package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.model.Coupon;
import com.team15.tripplanning.bookingservice.repository.CouponRepository;
import java.util.List;
import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + id));
    }

    public Coupon update(Long id, Coupon coupon) {
        Coupon existing = findById(id);
        existing.setCode(coupon.getCode());
        existing.setDiscountType(coupon.getDiscountType());
        existing.setDiscountValue(coupon.getDiscountValue());
        existing.setMaxUses(coupon.getMaxUses());
        existing.setCurrentUses(coupon.getCurrentUses());
        existing.setExpiryDate(coupon.getExpiryDate());
        existing.setActive(coupon.getActive());
        existing.setMetadata(coupon.getMetadata());
        return couponRepository.save(existing);
    }

    public void delete(Long id) {
        couponRepository.delete(findById(id));
    }
}
