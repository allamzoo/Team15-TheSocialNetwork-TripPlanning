package com.team15.tripplanning.bookingservice;

import com.team15.tripplanning.bookingservice.adapter.MongoDocumentAdapter;
import com.team15.tripplanning.bookingservice.dto.BookingDetailsDTO;
import com.team15.tripplanning.bookingservice.model.mongo.PaymentAuditEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoSqlAdapterTest {

    // ── a) MongoDocumentAdapter class exists ─────────────────────────────
    @Test
    void a_mongoDocumentAdapterExists() throws Exception {
        assertNotNull(Class.forName(
                "com.team15.tripplanning.bookingservice.adapter.MongoDocumentAdapter"));
    }

    // ── b) adapt() return type is BookingDetailsDTO ───────────────────────
    @Test
    void b_adaptMethodReturnsBookingDetailsDTO() throws Exception {
        Class<?> cls = Class.forName(
                "com.team15.tripplanning.bookingservice.adapter.MongoDocumentAdapter");
        Method adapt = cls.getMethod("adapt", PaymentAuditEvent.class);
        assertEquals(BookingDetailsDTO.class, adapt.getReturnType());
    }

    // ── c) MongoDocumentAdapter populates from PaymentAuditEvent ─────────
    @Test
    void c_mongoDocumentAdapterMapsPaymentEventToDto() {
        PaymentAuditEvent event = new PaymentAuditEvent(Map.of(
                "bookingId", 55L,
                "userId",    3L,
                "action",    "BOOKING_CONFIRMED",
                "method",    "CREDIT_CARD",
                "amount",    499.99
        ));

        MongoDocumentAdapter adapter = new MongoDocumentAdapter();
        BookingDetailsDTO dto = adapter.adapt(event);

        assertNotNull(dto);
        assertEquals(55L,                dto.getBookingId());
        assertEquals(3L,                 dto.getUserId());
        assertEquals(499.99,             dto.getOriginalAmount());
        assertEquals("BOOKING_CONFIRMED", dto.getStatus());
        assertNotNull(dto.getBookingDetails());
    }

    // ── c) MongoDocumentAdapter handles null amount safely ────────────────
    @Test
    void c_mongoDocumentAdapterHandlesNullAmount() {
        PaymentAuditEvent event = new PaymentAuditEvent(Map.of(
                "bookingId", 1L,
                "userId",    1L,
                "action",    "BOOKING_CANCELLED"
        ));

        MongoDocumentAdapter adapter = new MongoDocumentAdapter();
        BookingDetailsDTO dto = adapter.adapt(event);

        assertNotNull(dto);
        assertNull(dto.getOriginalAmount());
        assertEquals("BOOKING_CANCELLED", dto.getStatus());
    }
}
