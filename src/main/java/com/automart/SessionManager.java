package com.automart;

/**
 * SessionManager - Holds the currently logged-in user's session data.
 * Used by all components to check who is logged in and their role.
 */
public class SessionManager {

    private static String loggedInUserId   = null;
    private static String loggedInUsername = null;
    private static String loggedInRole     = null;  // "buyer" | "seller" | "admin"

    // ----- Login / Logout -----

    public static void login(String userId, String username, String role) {
        loggedInUserId   = userId;
        loggedInUsername = username;
        loggedInRole     = role;
    }

    public static void logout() {
        loggedInUserId   = null;
        loggedInUsername = null;
        loggedInRole     = null;
    }

    // ----- Checks -----

    public static boolean isLoggedIn() {
        return loggedInUserId != null;
    }

    public static boolean isAdmin() {
        return isLoggedIn() && "admin".equalsIgnoreCase(loggedInRole);
    }

    // ----- Getters -----

    public static String getLoggedInUserId()   { return loggedInUserId;   }
    public static String getLoggedInUsername() { return loggedInUsername; }
    public static String getLoggedInRole()     { return loggedInRole;     }
}
