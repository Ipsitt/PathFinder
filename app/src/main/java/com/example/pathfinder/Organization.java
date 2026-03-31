package com.example.pathfinder;

public class Organization {
    private String email, name, description, imageUri;

    public Organization(String email, String name, String description, String imageUri) {
        this.email = email;
        this.name = name;
        this.description = description;
        this.imageUri = imageUri;
    }

    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUri() { return imageUri; }
}