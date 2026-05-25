package com.automart;

// ============================================================
//  AutoMart Application  —  Component 01: User Management
//  FILE    : AdminUser.java
//  PURPOSE : Represents an administrator account.
//            Admins have elevated privileges:
//              • View all users in the admin dashboard
//              • Edit any user's contact details
//              • Ban / unban user accounts
//              • Monitor activity logs
//
//  OOP CONCEPTS USED:
//    • Inheritance  – extends AppUser, inherits all shared fields
//    • Polymorphism – overrides roleMessage() with admin-specific text
//    • Encapsulation – isAdmin() check inherited from AppUser
//
//  SIGNUP NOTE:
//    Admin accounts require a special invite code at registration
//    ("AUTO-MART-ADMIN" by default). This is enforced in UserManager.
// ============================================================

final class AdminUser extends AppUser {

    // ----------------------------------------------------------
    //  Constructor
    //  Calls super() with role = "admin"
    // ----------------------------------------------------------
    /**
     * Creates an admin account.
     *
     * @param username chosen admin login handle
     * @param email    Gmail address
     * @param phone    10-digit mobile number
     * @param password plain-text password (hashed inside AppUser)
     */
    AdminUser(String username, String email, String phone, String password) {
        super(username, email, phone, password, "admin");
    }

    // ----------------------------------------------------------
    //  Polymorphism — abstract method implementation
    // ----------------------------------------------------------
    /**
     * Returns an admin-specific role description.
     * Required by the abstract AppUser contract.
     */
    @Override
    String roleMessage() {
        return "Admin account: monitor users, listings, requests, and reviews from the control panel.";
    }

    // ----------------------------------------------------------
    //  toString  – useful for debugging
    // ----------------------------------------------------------
    @Override
    public String toString() {
        return "AdminUser{username=" + getUsername()
             + ", email=" + getEmail()
             + ", status=" + getStatus() + "}";
    }
}
