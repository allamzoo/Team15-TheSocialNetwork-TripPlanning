/**
 * M3 — Slice S3 (itinerary-service): Feign pre-check utilities.
 *
 * <p><b>OWNED BY: S3</b> — pull request {@code feature/m3-s3-feign-amqp-itinerary}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code UserActiveGuard} — throws {@code ResponseStatusException(409)} if
 *       {@code UserServiceClient.getUser(userId).status()} ≠ {@code "ACTIVE"}.
 *   <li>{@code DestinationActiveGuard} — throws {@code ResponseStatusException(409)} if
 *       {@code DestinationServiceClient.getDestination(destId).status()} ≠ {@code "ACTIVE"}.
 *   <li>{@code BookingCountGuard} — throws {@code ResponseStatusException(422)} if
 *       {@code BookingServiceClient.getConfirmedSummary(itineraryId).count()} {@code < 1}.
 * </ul>
 *
 * <p>Guards are injected into {@code saga.ItinerarySaga.SagaPreCheck} — they are not
 * controllers and should not be called from any other layer directly.
 */
package com.team15.tripplanning.itineraryservice.feature.precheck;
