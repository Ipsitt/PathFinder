package com.example.pathfinder;

// Data model for organization profile details.

public class Org {
    private String email, name, description, imageUri;

    // Creates an organization profile model.
    public Org(String email, String name, String description, String imageUri) {
        this.email = email;
        this.name = name;
        this.description = description;
        this.imageUri = imageUri;
    }

    // Returns the organization email.
    public String getEmail() { return email; }

    // Returns the organization name.
    public String getName() { return name; }

    // Returns the organization description.
    public String getDescription() { return description; }

    // Returns the organization image URI.
    public String getImageUri() { return imageUri; }
}
