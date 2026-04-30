package com.team15.tripplanning.bookingservice.adapter;

import com.team15.tripplanning.bookingservice.dto.BookingDetailsDTO;
import com.team15.tripplanning.bookingservice.model.mongo.PaymentAuditEvent;

public class MongoDocumentAdapter {

    public BookingDetailsDTO adapt(PaymentAuditEvent event) {
        return BookingDetailsDTO.builder()
                .bookingId(event.getBookingId())
                .userId(event.getUserId())
                .originalAmount(event.getAmount())
                .status(event.getAction())
                .bookingDetails(event.getDetails())
                .build();
    }
}
