package com.team15.tripplanning.shared.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}
