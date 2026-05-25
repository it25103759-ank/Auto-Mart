package com.automart;

// ============================================================
//  AutoMart Application  —  Component 06: Feedback & Review System
//  FILE    : ReviewEntry.java
//  PURPOSE : Data model for a single customer review.
//            Included here because SellerUser.calculateAverageRating()
//            depends on this class.
//
//  KEY FIELDS USED BY Component 01:
//    • authorUsername – matched against the seller's username
//    • rating         – parsed to int for average calculation
// ============================================================

class ReviewEntry {

    // ----------------------------------------------------------
    //  Fields  (package-private so other Component 01 classes can read them)
    // ----------------------------------------------------------
    final String id;              // unique review ID
    final String vehicleId;       // the car this review is for
    final String vehicleTitle;    // display title of the car
    final String authorUsername;  // who wrote the review
    String       comment;         // review text (mutable for edits)
    final String rating;          // star rating as string, e.g. "4"
    final String createdAt;       // ISO datetime string
    final String type;            // "VerifiedReview" or "PublicReview"

    // ----------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------
    ReviewEntry(String id, String vehicleId, String vehicleTitle,
                String authorUsername, String comment,
                String rating, String createdAt, String type) {
        this.id             = id;
        this.vehicleId      = vehicleId;
        this.vehicleTitle   = vehicleTitle;
        this.authorUsername = authorUsername;
        this.comment        = comment;
        this.rating         = rating;
        this.createdAt      = createdAt;
        this.type           = type;
    }

    // ----------------------------------------------------------
    //  File serialisation helpers
    // ----------------------------------------------------------

    /** Serialises the review to a tab-separated line for reviews.txt. */
    String toRecord() {
        return String.join("\t",
            AppUser.clean(type),
            AppUser.clean(id),
            AppUser.clean(vehicleId),
            AppUser.clean(vehicleTitle),
            AppUser.clean(authorUsername),
            AppUser.clean(comment),
            AppUser.clean(rating),
            AppUser.clean(createdAt)
        );
    }

    /** Parses one line from reviews.txt into a ReviewEntry. */
    static ReviewEntry fromRecord(String line) {
        if (line == null || line.isBlank()) return null;
        String[] p = line.split("\t", -1);
        if (p.length < 8) return null;
        return new ReviewEntry(p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[0]);
    }

    @Override
    public String toString() {
        return "ReviewEntry{id=" + id + ", author=" + authorUsername
             + ", rating=" + rating + "}";
    }
}
