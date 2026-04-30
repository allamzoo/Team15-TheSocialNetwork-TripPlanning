package com.team15.tripplanning.destinationservice.model.elasticsearch;

public class DestinationSearchDocument {
    private String id;
    private String name;
    private String country;
    private String category;
    private Double rating;
    private String status;

    public DestinationSearchDocument() {}

    public DestinationSearchDocument(String id, String name, String country,
                                     String category, Double rating, String status) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.category = category;
        this.rating = rating;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
