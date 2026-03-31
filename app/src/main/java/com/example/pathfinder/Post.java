package com.example.pathfinder;

import java.util.List;

public class Post {
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