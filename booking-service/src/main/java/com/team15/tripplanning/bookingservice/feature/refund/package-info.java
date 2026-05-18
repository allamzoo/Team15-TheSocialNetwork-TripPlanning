/**
 * M3 — Slice S5 (booking-service): Tier-based refund strategy.
 *
 * <p><b>OWNED BY: S5</b> — pull request {@code feature/m3-s5-feign-amqp-booking}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code RefundStrategy} — interface: {@code BigDecimal calculate(BigDecimal paid, long daysUntilStart)}.
 *   <li>{@code EarlyRefundStrategy} — {@code daysUntilStart > 14} → 100 % refund.
 *   <li>{@code LateRefundStrategy}  — {@code 7 < daysUntilStart ≤ 14} → 50 % refund.
 *   <li>{@code NoRefundStrategy}    — {@code daysUntilStart ≤ 7} → 0 % refund.
 *   <li>{@code RefundStrategyFactory} — selects strategy from {@code daysUntilStart};
 *       invoked by {@code saga.SettlementSaga} when a refund is requested.
 *   <li>{@code RefundTierController} — exposes:
 *       {@code POST /api/bookings/{bookingId}/refund-cancellation-tier}
 *       → {@code RefundTierDTO(refundAmount, tier)} (dry-run, no mutation).
 * </ul>
 *
 * <p>The existing {@code strategy} package contains M1 pricing strategies — do NOT
 * add refund strategies there; keep M3 refund work exclusively in this package.
 */
package com.team15.tripplanning.bookingservice.feature.refund;
