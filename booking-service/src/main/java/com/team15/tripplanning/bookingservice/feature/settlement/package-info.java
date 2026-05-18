/**
 * M3 — Slice S5 (booking-service): Settlement creation and processing.
 *
 * <p><b>OWNED BY: S5</b> — pull request {@code feature/m3-s5-feign-amqp-booking}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code SettlementController} — exposes:
 *       <ul>
 *         <li>{@code GET /api/bookings/settlements/{itineraryId}} → {@code SettlementDTO}
 *         <li>{@code GET /api/bookings/settlements} (ADMIN only) → paginated list
 *       </ul>
 *   <li>{@code SettlementService} — read-only queries against the {@code settlements}
 *       table. Mutation is exclusively handled by the saga in the {@code saga} package.
 *   <li>{@code SettlementDTO} — record({@code id, itineraryId, amount, status, createdAt, settledAt}).
 * </ul>
 *
 * <p>Do NOT put saga state transitions here; all mutation belongs in {@code saga.SettlementSaga}.
 */
package com.team15.tripplanning.bookingservice.feature.settlement;
