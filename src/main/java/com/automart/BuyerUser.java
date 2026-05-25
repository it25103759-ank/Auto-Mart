package com.automart;

// ============================================================
//  AutoMart Application  —  Component 01: User Management
//  FILE    : BuyerUser.java
//  PURPOSE : Represents a buyer account.
//            A buyer can browse the inventory, submit purchase
//            requests, and leave reviews on listings.
//
//  OOP CONCEPTS USED:
//    • Inheritance  – extends AppUser, inherits all shared fields
//    • Polymorphism – overrides roleMessage() with buyer-specific text
//    • Encapsulation – no additional fields exposed beyond AppUser
// ============================================================

final class BuyerUser extends AppUser {

    // ----------------------------------------------------------
    //  Constructor
    //  Calls super() with role = "buyer"
    // ----------------------------------------------------------
    /**
     * Creates a buyer account.
     *
     * @param username chosen login handle
     * @param email    Gmail address
     * @param phone    10-digit mobile number
     * @param password plain-text password (hashed inside AppUser)
     */
    BuyerUser(String username, String email, String phone, String password) {
        super(username, email, phone, password, "buyer");
    }

    // ----------------------------------------------------------
    //  Polymorphism — abstract method implementation
    // ----------------------------------------------------------
    /**
     * Returns a buyer-specific role description.
     * This method is required by the abstract AppUser contract.
     */
    @Override
    String roleMessage() {
        return "Buyer account: browse inventory, submit purchase requests, and leave reviews.";
    }

    // ----------------------------------------------------------
    //  toString  – useful for debugging
    // ----------------------------------------------------------
    @Override
    public String toString() {
        return "BuyerUser{username=" + getUsername()
             + ", email=" + getEmail()
             + ", status=" + getStatus() + "}";
    }
}
