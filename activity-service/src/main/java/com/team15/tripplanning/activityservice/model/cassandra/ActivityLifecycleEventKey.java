package com.team15.tripplanning.activityservice.model.cassandra;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

@PrimaryKeyClass
public class ActivityLifecycleEventKey implements Serializable {

    @PrimaryKeyColumn(name = "activity_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long activityId;

    @PrimaryKeyColumn(name = "event_timestamp", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Instant eventTimestamp;

    public ActivityLifecycleEventKey() {}

    public ActivityLifecycleEventKey(Long activityId, Instant eventTimestamp) {
        this.activityId = activityId;
        this.eventTimestamp = eventTimestamp;
    }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityLifecycleEventKey)) return false;
        ActivityLifecycleEventKey that = (ActivityLifecycleEventKey) o;
        return Objects.equals(activityId, that.activityId) && Objects.equals(eventTimestamp, that.eventTimestamp);
    }

    @Override
    public int hashCode() { return Objects.hash(activityId, eventTimestamp); }
}
