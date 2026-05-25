package com.automart;

// ============================================================
//  AutoMart Application  —  Component 01: User Management
//  FILE    : SessionManager.java
//  PURPOSE : Manages user login sessions via HTTP cookies.
//
//  HOW IT WORKS:
//    • When a user logs in successfully, startSession() is called.
//      This sets an HTTP-only cookie named "automart_session"
//      containing the user's username.
//    • On every page request, currentUsername() reads that cookie
//      to identify who is logged in.
//    • When a user logs out, clearSession() removes the cookie
//      by setting its Max-Age to 0.
//
//  SECURITY NOTES:
//    • HttpOnly prevents JavaScript from reading the cookie.
//    • SameSite=Lax protects against basic CSRF.
//    • The username stored in the cookie is URL-encoded to
//      handle special characters safely.
//
//  OOP CONCEPTS:
//    • Encapsulation – all methods are static utilities;
//      no mutable state is held in this class.
// ============================================================

import com.sun.net.httpserver.HttpExchange;
import java.util.List;
import java.util.UUID;

final class SessionManager {

    /** The name of the session cookie stored in the browser. */
    static final String AUTH_COOKIE = "automart_session";

    // ----------------------------------------------------------
    //  Private constructor — this class should never be instantiated
    // ----------------------------------------------------------
    private SessionManager() { }

    // ----------------------------------------------------------
    //  startSession()
    //  Called after a successful login.
    // ----------------------------------------------------------
    /**
     * Sets an HTTP-only session cookie containing the username.
     * If the username is blank, a random guest token is used instead.
     *
     * @param exchange  the current HTTP exchange
     * @param username  the authenticated user's username
     */
    static void startSession(HttpExchange exchange, String username) {
        // Sanitise the name and remove semicolons (which break Set-Cookie headers)
        String safeName = AppUser.safe(username).replace(";", "").trim();
        if (safeName.isBlank()) {
            // Fallback: assign a random guest token
            safeName = "guest-" + UUID.randomUUID().toString().substring(0, 8);
        }

        // URL-encode spaces as %20 so the cookie value is safe
        String encoded = safeName.replace(" ", "%20");

        exchange.getResponseHeaders().add(
            "Set-Cookie",
            AUTH_COOKIE + "=" + encoded + "; Path=/; HttpOnly; SameSite=Lax"
        );
    }

    // ----------------------------------------------------------
    //  clearSession()
    //  Called on logout.
    // ----------------------------------------------------------
    /**
     * Clears the session cookie by setting its Max-Age to 0.
     * This tells the browser to immediately delete the cookie.
     *
     * @param exchange  the current HTTP exchange
     */
    static void clearSession(HttpExchange exchange) {
        exchange.getResponseHeaders().add(
            "Set-Cookie",
            AUTH_COOKIE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"
        );
    }

    // ----------------------------------------------------------
    //  currentUsername()
    //  Called on every page request to identify the logged-in user.
    // ----------------------------------------------------------
    /**
     * Reads the session cookie from the incoming request and
     * returns the stored username.
     *
     * @param exchange  the current HTTP exchange
     * @return          the username string, or an empty string if not logged in
     */
    static String currentUsername(HttpExchange exchange) {
        return readCookie(exchange, AUTH_COOKIE);
    }

    // ----------------------------------------------------------
    //  Private helper – reads a named cookie from the request
    // ----------------------------------------------------------
    /**
     * Parses the "Cookie" request header and returns the value
     * of the cookie with the given name.
     *
     * @param exchange   the HTTP exchange
     * @param cookieName the cookie name to look for
     * @return           the cookie value (URL-decoded), or "" if not found
     */
    private static String readCookie(HttpExchange exchange, String cookieName) {
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");
        if (cookieHeaders == null) return "";

        for (String header : cookieHeaders) {
            // Each header may contain multiple cookies: "name1=val1; name2=val2"
            for (String entry : header.split(";")) {
                String[] parts = entry.trim().split("=", 2);
                if (parts.length == 2
                        && parts[0].trim().equals(cookieName)) {
                    // URL-decode the value (%20 → space, etc.)
                    return decode(parts[1].trim());
                }
            }
        }
        return "";
    }

    // ----------------------------------------------------------
    //  Private helper – basic URL-decode
    // ----------------------------------------------------------
    /**
     * Decodes a URL-encoded string.
     * Falls back to the original value if decoding fails.
     *
     * @param s  URL-encoded string
     * @return   decoded string
     */
    private static String decode(String s) {
        try {
            return java.net.URLDecoder.decode(
                    AppUser.safe(s),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return AppUser.safe(s);
        }
    }
}
