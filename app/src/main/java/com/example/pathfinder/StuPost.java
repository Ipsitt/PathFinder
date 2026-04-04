package com.example.pathfinder;

import java.util.List;

// Data model for student-facing internship posts.

public class StuPost {
    public int id;
    public String title;
    public String description;
    public String stipend;
    public String timePeriod;
    public String orgName;
    public String orgEmail;
    public byte[] orgImage;            // loaded separately
    public List<DBHelper.Tag> tags;    // full tag objects with label + color
}
