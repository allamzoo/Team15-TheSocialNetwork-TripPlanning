package com.team15.tripplanning.bookingservice.repository;

import com.team15.tripplanning.bookingservice.model.mongo.PaymentAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentAuditEventRepository extends MongoRepository<PaymentAuditEvent, String> {

    List<PaymentAuditEvent> findByBookingIdOrderByTimestampDesc(Long bookingId);

    Page<PaymentAuditEvent> findByBookingId(Long bookingId, Pageable pageable);

    List<PaymentAuditEvent> findByBookingIdAndActionNotOrderByTimestampAsc(Long bookingId, String excludedAction);
}
