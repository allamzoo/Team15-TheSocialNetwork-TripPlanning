package com.team15.tripplanning.userservice.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}
