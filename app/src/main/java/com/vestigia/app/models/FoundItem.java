package com.vestigia.app.models;

public class FoundItem {
    private int id;
    private String itemName;
    private String description;
    private String category;
    private String location;
    private String dateFound;
    private String status;
    private String imageUrl;
    private String reportedByName;

    public FoundItem(int id, String itemName, String description, String category,
                     String location, String dateFound, String status,
                     String imageUrl, String reportedByName) {
        this.id = id;
        this.itemName = itemName;
        this.description = description;
        this.category = category;
        this.location = location;
        this.dateFound = dateFound;
        this.status = status;
        this.imageUrl = imageUrl;
        this.reportedByName = reportedByName;
    }

    public int getId() { return id; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public String getDateFound() { return dateFound; }
    public String getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }
    public String getReportedByName() { return reportedByName; }
}