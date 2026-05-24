package com.automart;


import java.time.LocalDateTime;

public class ReviewEntry {
    private String reviewerName;
    private String reviewTitle;
    private String reviewBody;
    private int ratingStars;
    private LocalDateTime timestamp;

    public ReviewEntry(String reviewerName, String reviewTitle, String reviewBody, int ratingStars) {
        this.reviewerName = reviewerName;
        this.reviewTitle = reviewTitle;
        this.reviewBody = reviewBody;
        this.ratingStars = Math.max(1, Math.min(5, ratingStars)); // Keeps it between 1-5 stars
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String name) { this.reviewerName = name; }

    public String getReviewTitle() { return reviewTitle; }
    public void setReviewTitle(String title) { this.reviewTitle = title; }

    public String getReviewBody() { return reviewBody; }
    public void setReviewBody(String body) { this.reviewBody = body; }

    public int getRatingStars() { return ratingStars; }
    public void setRatingStars(int stars) { this.ratingStars = stars; }

    public LocalDateTime getTimestamp() { return timestamp; }
}