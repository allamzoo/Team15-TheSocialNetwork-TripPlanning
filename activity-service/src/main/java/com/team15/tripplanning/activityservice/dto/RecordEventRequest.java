package com.team15.tripplanning.activityservice.dto;

public class RecordEventRequest {
    private String status;
    private String notes;

    public RecordEventRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
