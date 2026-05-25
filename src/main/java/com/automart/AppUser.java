package com.automart;

// ============================================================
//  AutoMart Application  —  Component 01: User Management
//  FILE    : AppUser.java
//  PURPOSE : Abstract base class for ALL user types.
//            Encapsulates every field that every user shares:
//            username, email, phone, passwordHash, role,
//            status, banReason, adminNote.
//
//  OOP CONCEPTS USED:
//    • Encapsulation  – all fields private, accessed via getters/setters
//    • Abstraction    – class is abstract; cannot be instantiated directly
//    • Inheritance    – BuyerUser, SellerUser, AdminUser all extend this
//    • Polymorphism   – roleMessage() is abstract; each subclass overrides it
//
//  FILE FORMAT (users.txt — tab-separated):
//    role \t username \t email \t phone \t passwordHash \t status \t banReason \t adminNote
//    e.g.  buyer \t buyer1 \t buyer1@gmail.com \t 0770000001 \t salt$... \t active \t \t
// ============================================================

import java.util.Locale;

abstract class AppUser {

    // ----------------------------------------------------------
    //  Private fields  (Encapsulation)
    // ----------------------------------------------------------
    private String username;       // login handle, e.g. "buyer1"
    private String email;          // Gmail address
    private String phone;          // 10-digit number
    private String passwordHash;   // salted hash stored in file
    private final String role;     // "buyer" | "seller" | "admin"  (immutable after creation)
    private String status;         // "active" | "banned"
    private String banReason;      // reason text if banned
    private String adminNote;      // internal admin annotation

    // ----------------------------------------------------------
    //  Constructor
    //  Called by every subclass via super(...)
    // ----------------------------------------------------------
    /**
     * Creates a new AppUser.
     *
     * @param username plain username
     * @param email    Gmail address
     * @param phone    10-digit phone
     * @param password plain-text password (will be hashed before storing)
     * @param role     "buyer", "seller", or "admin"
     */
    AppUser(String username, String email, String phone, String password, String role) {
        this.username     = safe(username).trim();
        this.email        = safe(email).trim();
        this.phone        = safe(phone).trim();
        this.passwordHash = hashPassword(password);   // never store plain-text passwords
        this.role         = role;
        this.status       = "active";
        this.banReason    = "";
        this.adminNote    = "";
    }

    // ----------------------------------------------------------
    //  Getters
    // ----------------------------------------------------------
    String getUsername()   { return username; }
    String getEmail()      { return email; }
    String getPhone()      { return phone; }
    String getPassword()   { return passwordHash; }
    String getRole()       { return role; }

    /** Returns "active" as the default if status is blank/null. */
    String getStatus()     { return (status == null || status.isBlank()) ? "active" : status; }
    String getBanReason()  { return banReason == null ? "" : banReason; }
    String getAdminNote()  { return adminNote == null ? "" : adminNote; }

    // ----------------------------------------------------------
    //  Setters
    // ----------------------------------------------------------
    void setUsername(String username)   { this.username  = safe(username).trim(); }
    void setEmail(String email)         { this.email     = safe(email).trim(); }
    void setPhone(String phone)         { this.phone     = safe(phone).trim(); }
    void setBanReason(String reason)    { this.banReason = safe(reason).trim(); }
    void setAdminNote(String note)      { this.adminNote = safe(note).trim(); }

    /** Sets status; defaults to "active" if blank. */
    void setStatus(String status) {
        String s = safe(status).trim().toLowerCase(Locale.ROOT);
        this.status = s.isBlank() ? "active" : s;
    }

    /** Hashes and stores a new password. */
    void setPassword(String password) {
        this.passwordHash = hashPassword(password);
    }

    // ----------------------------------------------------------
    //  Business methods
    // ----------------------------------------------------------

    /** Returns true if the supplied plain-text password matches the stored hash. */
    boolean checkPassword(String candidate) {
        return hashPassword(candidate).equals(passwordHash);
    }

    /** Returns true if this user's status is "banned". */
    boolean isBanned() {
        return "banned".equalsIgnoreCase(getStatus());
    }

    /** Returns true if this user holds the admin role. */
    boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    // ----------------------------------------------------------
    //  File serialisation / deserialisation
    // ----------------------------------------------------------

    /**
     * Converts this user to a single tab-separated line for users.txt.
     * Format:
     *   role TAB username TAB email TAB phone TAB passwordHash TAB status TAB banReason TAB adminNote
     */
    String toRecord() {
        return String.join("\t",
            clean(role),
            clean(username),
            clean(email),
            clean(phone),
            clean(passwordHash),
            clean(getStatus()),
            clean(getBanReason()),
            clean(getAdminNote())
        );
    }

    /**
     * Parses one tab-separated line from users.txt and returns
     * the correct subclass (BuyerUser / SellerUser / AdminUser).
     *
     * @param line one line from the file
     * @return an AppUser subclass instance, or null if the line is invalid
     */
    static AppUser fromRecord(String line) {
        if (line == null || line.isBlank()) return null;
        String[] p = line.split("\t", -1);       // -1 keeps trailing empty fields
        if (p.length < 5) return null;           // minimum: role,user,email,phone,hash

        // Build the correct subclass based on role (column 0)
        AppUser user = switch (p[0].trim().toLowerCase(Locale.ROOT)) {
            case "admin"  -> new AdminUser (p[1], p[2], p[3], p[4]);
            case "seller" -> new SellerUser(p[1], p[2], p[3], p[4]);
            default       -> new BuyerUser (p[1], p[2], p[3], p[4]);
        };

        // password stored as hash; re-assign directly so it isn't double-hashed
        user.passwordHash = safe(p[4]).trim();

        // Optional extended columns
        if (p.length > 5 && !p[5].isBlank()) user.setStatus(p[5]);
        if (p.length > 6)                     user.setBanReason(p[6]);
        if (p.length > 7)                     user.setAdminNote(p[7]);

        return user;
    }

    // ----------------------------------------------------------
    //  Abstract method  (Polymorphism)
    // ----------------------------------------------------------
    /**
     * Each subclass provides its own role description.
     * This forces BuyerUser, SellerUser, and AdminUser to implement it.
     */
    abstract String roleMessage();

    // ----------------------------------------------------------
    //  toString  – useful for debugging
    // ----------------------------------------------------------
    @Override
    public String toString() {
        return "AppUser{role=" + role
             + ", username=" + username
             + ", email=" + email
             + ", status=" + getStatus() + "}";
    }

    // ----------------------------------------------------------
    //  Private static helpers  (copied from AutoMartApplication)
    // ----------------------------------------------------------

    /**
     * Returns empty string if s is null, otherwise trims it.
     * Prevents NullPointerExceptions throughout the class.
     */
    static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Replaces tab characters in a value so a tab-delimited file
     * is never broken by user-supplied data.
     */
    static String clean(String s) {
        return safe(s).replace("\t", " ").replace("\n", " ").replace("\r", "");
    }

    /**
     * Hashes a plain-text password with a simple salted hash.
     * If the value already looks hashed (starts with "salt$"), it is returned as-is.
     * This prevents double-hashing when reading records back from the file.
     *
     * @param raw plain-text password
     * @return   "salt$" + hex of the salted hashCode
     */
    static String hashPassword(String raw) {
        String clean = safe(raw).trim();
        // Already hashed  →  return as-is (happens when loading from file)
        if (clean.startsWith("salt$")) return clean;
        return "salt$" + Integer.toHexString(("AutoMartSalt::" + clean).hashCode());
    }
}
