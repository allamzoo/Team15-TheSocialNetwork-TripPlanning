/**
 * M3 — Slice S5 (booking-service): Settlement saga state machine.
 *
 * <p><b>OWNED BY: S5</b> — pull request {@code feature/m3-s5-feign-amqp-booking}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code SettlementSaga} — {@code @Service} driving the payment saga:
 *       <pre>
 *       ItineraryCompletedEvent received
 *           → Settlement created with status SETTLEMENT_PENDING
 *           → PaymentInitiatedEvent published
 *       PaymentCompletedEvent received (from payment gateway simulation)
 *           → Settlement → SETTLED
 *           → PaymentCompletedEvent republished to itinerary.events callback queue
 *       PaymentFailedEvent received
 *           → Settlement → PAYMENT_FAILED
 *           → PaymentFailedEvent republished to itinerary.events callback queue
 *       Refund requested (DELETE /api/bookings/{id} with confirmed status)
 *           → RefundStrategy applied → Settlement → REFUNDED
 *           → PaymentRefundedEvent published
 *       </pre>
 * </ul>
 *
 * <p>The {@code Settlement} JPA entity lives in {@code model.Settlement} — add it in this PR.
 * Its migration DDL ({@code V3__add_settlements.sql}) goes in
 * {@code src/main/resources/db/migration/}.
 * The unique index on {@code itinerary_id} is the idempotency guard for duplicate messages.
 */
package com.team15.tripplanning.bookingservice.saga;
