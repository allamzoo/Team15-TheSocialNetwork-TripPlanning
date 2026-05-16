package com.team15.tripplanning.userservice.adapter;

import com.team15.tripplanning.contracts.dto.UserTripSummaryAggregateDTO;
import com.team15.tripplanning.userservice.dto.UserTripSummaryDTO;
import com.team15.tripplanning.userservice.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ObjectArrayDtoAdapter {

    public UserTripSummaryDTO adapt(User user, UserTripSummaryAggregateDTO summary) {
        return UserTripSummaryDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .totalTrips(summary.totalTrips())
                .completedTrips(summary.completedTrips())
                .cancelledTrips(summary.cancelledTrips())
                .totalSpent(summary.totalBudget() != null ? summary.totalBudget().doubleValue() : 0.0)
                .averageBudget(summary.averageBudget() != null ? summary.averageBudget().doubleValue() : 0.0)
                .build();
    }

    public UserTripSummaryDTO adapt(Object rawRow) {
        Object[] row = unwrapRow(rawRow);
        return UserTripSummaryDTO.builder()
                .userId(toLong(row[0]))
                .name(row[1] != null ? row[1].toString() : null)
                .totalTrips(toLong(row[2]))
                .completedTrips(toLong(row[3]))
                .cancelledTrips(toLong(row[4]))
                .totalSpent(toDouble(row[5]))
                .averageBudget(toDouble(row[6]))
                .build();
    }

    private Object[] unwrapRow(Object rawRow) {
        if (rawRow instanceof Object[] row) {
            if (row.length == 1 && row[0] instanceof Object[] nestedRow) {
                return nestedRow;
            }
            return row;
        }
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected trip summary row format"
        );
    }

    private Long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private Double toDouble(Object value) {
        return value == null ? 0.0 : ((Number) value).doubleValue();
    }
}
