package com.vestigia.app.models;

public class RecentPost {
    private int id;
    private String itemType;
    private String itemName;
    private String location;
    private String imageUrl;
    private String reporterName;

    public RecentPost(int id, String itemType, String itemName, String location, String imageUrl, String reporterName) {
        this.id = id;
        this.itemType = itemType;
        this.itemName = itemName;
        this.location = location;
        this.imageUrl = imageUrl;
        this.reporterName = reporterName;
    }

    public int getId() { return id; }
    public String getItemType() { return itemType; }
    public String getItemName() { return itemName; }
    public String getLocation() { return location; }
    public String getImageUrl() { return imageUrl; }
    public String getReporterName() { return reporterName; }
}