/**
 * M3 Day-0 — Feign fallback implementations.
 *
 * <p>Each class implements the corresponding {@code contracts.feign.*Client} interface
 * and returns <b>safe defaults</b> for every method. They are used by service layers
 * to gracefully degrade when a downstream M3 endpoint has not been implemented yet
 * (the publisher slice hasn't merged or hasn't been deployed).
 *
 * <h3>Why no circuit breaker?</h3>
 * <p>These are plain Java implementations — no Resilience4j or Spring Cloud
 * CircuitBreaker dependency required. The pattern used in each service is:
 * <pre>
 *   try {
 *       return itineraryServiceClient.getUserItinerarySummary(userId);
 *   } catch (FeignException e) {
 *       log.warn("itinerary-service M3 endpoint unavailable — using fallback");
 *       return ItineraryServiceFallback.SAFE.getUserItinerarySummary(userId);
 *   }
 * </pre>
 *
 * <h3>Fallback philosophy</h3>
 * <ul>
 *   <li>Display/aggregate methods → return empty/zero value so the caller keeps working.
 *   <li>Validation/guard methods (existence checks, saga pre-checks) → return a value
 *       that makes the guard <em>fail safely</em> (e.g., {@code count=0} prevents saga
 *       trigger; {@code null} destination forces callers to reject the request).
 * </ul>
 *
 * <p><b>OWNED BY: contracts module (Day-0 kickoff)</b> — these classes are committed
 * on Day-0 and must not be modified by feature slice PRs.
 */
package com.team15.tripplanning.contracts.feign.fallback;
