package com.automart;

// ============================================================
//  AutoMart Application  —  Component 01: User Management
//  FILE    : SellerUser.java
//  PURPOSE : Represents a seller account.
//            A seller can post car listings, manage their
//            inventory, approve/reject buyer requests, and
//            respond to reviews.
//
//  OOP CONCEPTS USED:
//    • Inheritance  – extends AppUser, inherits all shared fields
//    • Polymorphism – overrides roleMessage() with seller-specific text
//    • Encapsulation – calculateAverageRating() adds seller-only behaviour
// ============================================================

import java.util.List;

final class SellerUser extends AppUser {

    // ----------------------------------------------------------
    //  Constructor
    //  Calls super() with role = "seller"
    // ----------------------------------------------------------
    /**
     * Creates a seller account.
     *
     * @param username    chosen login handle / business name
     * @param email       Gmail address
     * @param phone       10-digit mobile number
     * @param password    plain-text password (hashed inside AppUser)
     */
    SellerUser(String username, String email, String phone, String password) {
        super(username, email, phone, password, "seller");
    }

    // ----------------------------------------------------------
    //  Seller-specific behaviour
    // ----------------------------------------------------------
    /**
     * Calculates the average star rating for this seller
     * by scanning a provided list of ReviewEntry objects.
     *
     * @param reviews the full list of reviews in the system
     * @return        average rating (0.0 – 5.0), or 0.0 if none exist
     */
    double calculateAverageRating(List<ReviewEntry> reviews) {
        return reviews.stream()
                      // only include reviews written BY this seller's username
                      .filter(r -> r.authorUsername.equalsIgnoreCase(getUsername()))
                      // parse the rating string to int, default 0 if malformed
                      .mapToInt(r -> parseIntSafe(r.rating, 0))
                      .average()
                      .orElse(0.0);
    }

    // ----------------------------------------------------------
    //  Polymorphism — abstract method implementation
    // ----------------------------------------------------------
    /**
     * Returns a seller-specific role description.
     * Required by the abstract AppUser contract.
     */
    @Override
    String roleMessage() {
        return "Seller account: manage listings, approve purchase requests, and respond to buyers.";
    }

    // ----------------------------------------------------------
    //  toString  – useful for debugging
    // ----------------------------------------------------------
    @Override
    public String toString() {
        return "SellerUser{username=" + getUsername()
             + ", email=" + getEmail()
             + ", status=" + getStatus() + "}";
    }

    // ----------------------------------------------------------
    //  Private helper  (mirrors AutoMartApplication.parseIntSafe)
    // ----------------------------------------------------------
    private static int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(AppUser.safe(value).trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
