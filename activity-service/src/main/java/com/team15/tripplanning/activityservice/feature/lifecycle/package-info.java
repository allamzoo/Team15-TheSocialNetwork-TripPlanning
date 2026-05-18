/**
 * M3 — Slice S4 (activity-service): Cassandra lifecycle-recording feature.
 *
 * <p><b>OWNED BY: S4</b> — pull request {@code feature/m3-s4-feign-amqp-activity}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code ActivityLifecycleController} — exposes:
 *       <ul>
 *         <li>{@code POST /api/activities/{activityId}/lifecycle}
 *             body: {@code \{status, notes\}} → {@code 201}
 *             Appends a row to {@code activity_lifecycle} in Cassandra (partition key:
 *             {@code activityId}, clustering key: {@code recordedAt DESC}).
 *         <li>{@code GET /api/activities/{activityId}/lifecycle}
 *             → {@code List<ActivityLifecycleDTO>} (ordered by {@code recordedAt DESC})
 *       </ul>
 *   <li>{@code ActivityLifecycleService} — orchestrates:
 *       (1) Feign call {@code ItineraryServiceClient.getItinerary} to confirm the parent
 *       itinerary is PLANNED or IN_PROGRESS before accepting a lifecycle entry.
 *       (2) Writes to Cassandra via the repository in {@code model.cassandra}.
 *       (3) Publishes {@code ActivityLifecycleRecordedEvent}.
 *   <li>{@code ActivityLifecycleDTO} — record({@code activityId, status, notes, recordedAt}).
 * </ul>
 *
 * <p>The Cassandra entity ({@code ActivityLifecycleRecord}) and its repository live in
 * {@code model.cassandra} and {@code repository} respectively — do NOT duplicate them here.
 */
package com.team15.tripplanning.activityservice.feature.lifecycle;
