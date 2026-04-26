package com.team15.tripplanning.itineraryservice.dto;

public class ItineraryDayDTO {

    private Long id;
    private int dayOrder;
    private String date;
    private String title;
    private String description;
    private String status;
    private Object metadata;

    // ✅ Default constructor
    public ItineraryDayDTO() {
    }

    // ✅ Full constructor
    public ItineraryDayDTO(Long id, int dayOrder, String date,
                           String title, String description,
                           String status, Object metadata) {
        this.id = id;
        this.dayOrder = dayOrder;
        this.date = date;
        this.title = title;
        this.description = description;
        this.status = status;
        this.metadata = metadata;
    }

    // ✅ Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getDayOrder() {
        return dayOrder;
    }

    public void setDayOrder(int dayOrder) {
        this.dayOrder = dayOrder;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
}