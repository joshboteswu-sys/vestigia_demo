package com.vestigia.app.models;

public class LostItem {
    private int id;
    private String itemName;
    private String description;
    private String category;
    private String lastSeenLocation;
    private String dateLost;
    private String status;
    private String imageUrl;
    private int reportedBy;

    public LostItem(int id, String itemName, String description, String category,
                    String lastSeenLocation, String dateLost, String status,
                    String imageUrl, int reportedBy) {
        this.id = id;
        this.itemName = itemName;
        this.description = description;
        this.category = category;
        this.lastSeenLocation = lastSeenLocation;
        this.dateLost = dateLost;
        this.status = status;
        this.imageUrl = imageUrl;
        this.reportedBy = reportedBy;
    }

    public int getId() { return id; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getLastSeenLocation() { return lastSeenLocation; }
    public String getDateLost() { return dateLost; }
    public String getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }
    public int getReportedBy() { return reportedBy; }
}