package com.example.myapplication.model;

public class Book {
    private String id;
    private String title;
    private String author;
    private String narrator;
    private String description;
    private String category;
    private String coverUrl;
    private int totalDuration;
    private boolean premium;

    public Book(String id, String title, String author, String narrator,
                String description, String category, String coverUrl,
                int totalDuration, boolean premium) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.narrator = narrator;
        this.description = description;
        this.category = category;
        this.coverUrl = coverUrl;
        this.totalDuration = totalDuration;
        this.premium = premium;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getNarrator() { return narrator; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getCoverUrl() { return coverUrl; }
    public int getTotalDuration() { return totalDuration; }
    public boolean isPremium() { return premium; }
}