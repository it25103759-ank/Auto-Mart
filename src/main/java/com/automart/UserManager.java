package com.automart;

// ============================================================
//  AutoMart Application  —  Component 01: User Management
//  FILE    : UserManager.java
//  PURPOSE : Handles ALL CRUD operations for user accounts.
//            Reads from and writes to  src/main/resources/data/users.txt.
//
//  CRUD OPERATIONS:
//    CREATE  → registerUser()       Register buyer / seller
//            → registerAdminUser()  Register admin with invite code
//    READ    → loadUsers()          Load all users from file
//            → findUser()           Find by username
//            → findUserByEmail()    Find by email
//            → authenticateUser()   Login: match credentials
//    UPDATE  → updateCurrentUser()  User edits own profile
//            → updateUserFromAdmin() Admin edits any user's contact
//            → updateUserStatus()   Admin bans / unbans user
//    DELETE  → deleteUser()         Remove a user from the file
//
//  FILE FORMAT (users.txt  —  tab-separated, one user per line):
//    role \t username \t email \t phone \t passwordHash \t status \t banReason \t adminNote
//
//  OOP CONCEPTS:
//    • Inheritance   – extends BaseManager (which holds the file path)
//    • Encapsulation – all methods work on private List<AppUser>
//    • Polymorphism  – AppUser.fromRecord() returns the right subclass
// ============================================================

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class UserManager extends BaseManager {

    // ----------------------------------------------------------
    //  Validation patterns
    // ----------------------------------------------------------
    /** Email must be a valid Gmail address: letters/numbers/dots/underscores before @gmail.com */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-z0-9._]+@gmail\\.com$");

    /** Password must have ≥8 chars, at least one uppercase, one lowercase, one digit. */
    static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    /** Admin invite code required to create an admin account. */
    private static final String ADMIN_INVITE_CODE = "AUTO-MART-ADMIN";

    // ----------------------------------------------------------
    //  Path to the activity log
    // ----------------------------------------------------------
    private static final Path LOGS_FILE =
            Paths.get("src", "main", "resources", "data", "logs.txt");

    // ----------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------
    /**
     * Creates a UserManager pointing at users.txt.
     * The file path is passed up to BaseManager.
     */
    UserManager() {
        super(Paths.get("src", "main", "resources", "data", "users.txt"));
    }

    // ==========================================================
    //  CREATE
    // ==========================================================

    /**
     * Registers a new buyer or seller account.
     *
     * Steps:
     *   1. Validate all input fields.
     *   2. Check the username is not already taken.
     *   3. Create the correct AppUser subclass.
     *   4. Append the serialised record to users.txt.
     *   5. Log the activity.
     *
     * @param username chosen login name
     * @param email    Gmail address
     * @param phone    10-digit number
     * @param password plain-text password
     * @param role     "buyer" or "seller"
     * @return null on success; an error message string on failure
     */
    String registerUser(String username, String email,
                        String phone, String password, String role) throws IOException {

        // Clean inputs
        username = AppUser.safe(username).trim();
        email    = normalizeEmail(email);
        phone    = AppUser.safe(phone).trim();
        password = AppUser.safe(password).trim();

        // --- Validation ---
        if (username.isBlank())
            return "Username is required.";
        if (findUser(username) != null)
            return "That username is already taken.";

        String emailError = validateEmail(email);
        if (emailError != null) return emailError;

        // Check email uniqueness
        if (findUserByEmail(email) != null)
            return "An account with that email already exists.";

        String phoneError = validatePhone(phone);
        if (phoneError != null) return phoneError;

        // Remove any non-digit characters from phone before saving
        phone = digitsOnly(phone);

        if (!STRONG_PASSWORD_PATTERN.matcher(password).matches())
            return "Password must be at least 8 characters with uppercase, lowercase, and a number.";

        // --- Create the correct subclass ---
        AppUser user = switch (AppUser.safe(role).toLowerCase(Locale.ROOT)) {
            case "seller" -> new SellerUser(username, email, phone, password);
            default       -> new BuyerUser (username, email, phone, password);
        };

        // --- Append to file ---
        Files.writeString(dataFile,
                user.toRecord() + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        logActivity("USER_REGISTERED", username + " registered as " + user.getRole());
        return null; // null = success
    }

    /**
     * Registers a new admin account.
     * Requires a valid invite code in addition to all other checks.
     *
     * @param username   admin username
     * @param email      Gmail address
     * @param phone      10-digit number
     * @param password   plain-text password
     * @param adminCode  must equal ADMIN_INVITE_CODE
     * @return null on success; error message on failure
     */
    String registerAdminUser(String username, String email,
                             String phone, String password,
                             String adminCode) throws IOException {

        username  = AppUser.safe(username).trim();
        email     = normalizeEmail(email);
        phone     = AppUser.safe(phone).trim();
        password  = AppUser.safe(password).trim();
        adminCode = AppUser.safe(adminCode).trim();

        // Invite code gate
        if (!ADMIN_INVITE_CODE.equals(adminCode))
            return "Invalid admin invite code.";

        if (username.isBlank())
            return "Admin username is required.";
        if (findUser(username) != null)
            return "That admin username is already taken.";

        String emailError = validateEmail(email);
        if (emailError != null) return emailError;

        String phoneError = validatePhone(phone);
        if (phoneError != null) return phoneError;

        phone = digitsOnly(phone);

        if (!STRONG_PASSWORD_PATTERN.matcher(password).matches())
            return "Password must be at least 8 characters with uppercase, lowercase, and a number.";

        AdminUser admin = new AdminUser(username, email, phone, password);
        Files.writeString(dataFile,
                admin.toRecord() + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        logActivity("ADMIN_REGISTERED", username + " created an admin account");
        return null;
    }

    // ==========================================================
    //  READ
    // ==========================================================

    /**
     * Loads every user from users.txt into an ArrayList.
     * Blank lines are silently skipped.
     *
     * @return list of all AppUser objects (may be empty)
     */
    List<AppUser> loadUsers() throws IOException {
        List<AppUser> users = new ArrayList<>();
        if (!Files.exists(dataFile)) return users;

        for (String line : Files.readAllLines(dataFile, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            AppUser user = AppUser.fromRecord(line);
            if (user != null) users.add(user);
        }
        return users;
    }

    /**
     * Finds a user by their username (case-insensitive).
     *
     * @param username the username to look up
     * @return the matching AppUser, or null if not found
     */
    AppUser findUser(String username) throws IOException {
        String target = AppUser.safe(username).trim();
        for (AppUser user : loadUsers()) {
            if (user.getUsername().equalsIgnoreCase(target))
                return user;
        }
        return null;
    }

    /**
     * Finds a user by their email address (case-insensitive).
     *
     * @param email the email to look up
     * @return the matching AppUser, or null if not found
     */
    AppUser findUserByEmail(String email) throws IOException {
        String target = normalizeEmail(email);
        for (AppUser user : loadUsers()) {
            if (user.getEmail().equalsIgnoreCase(target))
                return user;
        }
        return null;
    }

    /**
     * Authenticates a login attempt.
     * Accepts either a username OR an email as the first credential.
     * Returns null if:
     *   • the user does not exist
     *   • the account is banned
     *   • the password is wrong
     *
     * @param usernameOrEmail login identifier
     * @param password        plain-text password to verify
     * @return the matching AppUser on success, or null on failure
     */
    AppUser authenticateUser(String usernameOrEmail, String password) throws IOException {
        String login = AppUser.safe(usernameOrEmail).trim();

        // Try username first
        AppUser user = findUser(login);

        // Try email if username lookup failed
        if (user == null && login.contains("@")) {
            user = findUserByEmail(normalizeEmail(login));
        }

        // Banned user cannot log in
        if (user != null && user.isBanned()) {
            logActivity("LOGIN_BLOCKED_BANNED",
                    user.getUsername() + " attempted login while banned");
            return null;
        }

        // Password check
        if (user != null && user.checkPassword(password)) {
            logActivity("LOGIN_SUCCESS", user.getUsername() + " logged in");
            return user;
        }

        logActivity("LOGIN_FAILED", AppUser.safe(usernameOrEmail) + " failed login attempt");
        return null;
    }

    // ==========================================================
    //  UPDATE
    // ==========================================================

    /**
     * Allows a user to edit their own username, email, phone, and/or password.
     * Validates all new values before saving.
     * If the username changes, the session must be refreshed by the caller.
     *
     * @param currentUsername the logged-in user's current username
     * @param newUsername     new username (or same as current to keep it)
     * @param newEmail        new email
     * @param newPhone        new phone
     * @param newPassword     new password, or empty string to keep existing
     * @return null on success; error message on failure
     */
    String updateCurrentUser(String currentUsername,
                             String newUsername, String newEmail,
                             String newPhone,   String newPassword) throws IOException {

        newUsername = AppUser.safe(newUsername).trim();
        newEmail    = normalizeEmail(newEmail);
        newPhone    = AppUser.safe(newPhone).trim();
        newPassword = AppUser.safe(newPassword).trim();

        // Validate email and phone
        String emailError = validateEmail(newEmail);
        if (emailError != null) return emailError;

        String phoneError = validatePhone(newPhone);
        if (phoneError != null) return phoneError;

        newPhone = digitsOnly(newPhone);

        // Validate new password only if one was supplied
        if (!newPassword.isBlank()) {
            if (!STRONG_PASSWORD_PATTERN.matcher(newPassword).matches())
                return "Password must be at least 8 characters with uppercase, lowercase, and a number.";
        }

        // Load all users, update the matching one, then rewrite the file
        List<AppUser> users = loadUsers();
        boolean found = false;

        for (AppUser user : users) {
            if (user.getUsername().equalsIgnoreCase(AppUser.safe(currentUsername).trim())) {
                user.setUsername(newUsername);
                user.setEmail(newEmail);
                user.setPhone(newPhone);
                if (!newPassword.isBlank()) {
                    user.setPassword(newPassword);
                }
                found = true;
                break;
            }
        }

        if (!found) return "User not found.";

        // Rewrite the entire file with updated data
        saveAllUsers(users);
        logActivity("USER_UPDATED", currentUsername + " updated their profile");
        return null; // success
    }

    /**
     * Allows an admin to update any user's email and phone number.
     *
     * @param username the target user's username
     * @param newEmail new email
     * @param newPhone new phone
     * @return null on success; error message on failure
     */
    String updateUserFromAdmin(String username,
                               String newEmail, String newPhone) throws IOException {

        newEmail = normalizeEmail(newEmail);
        newPhone = AppUser.safe(newPhone).trim();

        String emailError = validateEmail(newEmail);
        if (emailError != null) return emailError;

        String phoneError = validatePhone(newPhone);
        if (phoneError != null) return phoneError;

        newPhone = digitsOnly(newPhone);

        List<AppUser> users = loadUsers();
        boolean updated = false;

        for (AppUser user : users) {
            if (user.getUsername().equalsIgnoreCase(AppUser.safe(username).trim())) {
                user.setEmail(newEmail);
                user.setPhone(newPhone);
                updated = true;
                break;
            }
        }

        if (!updated) return "User not found.";

        saveAllUsers(users);
        logActivity("ADMIN_USER_UPDATED", "Admin edited contact details for " + username);
        return null;
    }

    /**
     * Sets a user's status (active / banned) and optional notes.
     * Used by admins to ban or reinstate accounts.
     *
     * @param usernameOrEmail target user identifier
     * @param status          "active" or "banned"
     * @param banReason       reason text (relevant when banning)
     * @param adminNote       internal admin annotation
     * @return true if the user was found and updated; false otherwise
     */
    boolean updateUserStatus(String usernameOrEmail,
                             String status, String banReason,
                             String adminNote) throws IOException {

        String target = AppUser.safe(usernameOrEmail).trim();
        if (target.isBlank()) return false;

        List<AppUser> users = loadUsers();
        boolean updated = false;

        for (AppUser user : users) {
            if (user.getUsername().equalsIgnoreCase(target)
                    || user.getEmail().equalsIgnoreCase(target)) {
                user.setStatus(status);
                user.setBanReason(banReason);
                user.setAdminNote(adminNote);
                updated = true;
                logActivity("USER_STATUS_UPDATED",
                        user.getUsername() + " status set to " + user.getStatus());
                break;
            }
        }

        if (updated) saveAllUsers(users);
        return updated;
    }

    // ==========================================================
    //  DELETE
    // ==========================================================

    /**
     * Permanently removes a user account from users.txt.
     *
     * @param username the username of the account to delete
     */
    void deleteUser(String username) throws IOException {
        String target = AppUser.safe(username).trim();
        List<AppUser> users = loadUsers();

        // Keep everyone except the target user
        List<AppUser> remaining = new ArrayList<>();
        for (AppUser user : users) {
            if (!user.getUsername().equalsIgnoreCase(target)) {
                remaining.add(user);
            }
        }

        saveAllUsers(remaining);
        logActivity("USER_DELETED", target + " account deleted");
    }

    // ==========================================================
    //  FILE SEEDING  (runs once if users.txt is empty)
    // ==========================================================

    /**
     * Seeds users.txt with three default demo accounts if the file
     * does not yet exist or is empty.
     * Accounts:  admin / admin123   |  buyer1 / buyer123   |  seller1 / seller123
     */
    void seedUsers() throws IOException {
        if (Files.exists(dataFile) && Files.size(dataFile) > 0) return;

        List<String> lines = List.of(
                new AdminUser ("admin",   "admin.automart@gmail.com", "0110000000", "admin123") .toRecord(),
                new BuyerUser ("buyer1",  "buyer1@gmail.com",         "0770000001", "buyer123") .toRecord(),
                new SellerUser("seller1", "seller1@gmail.com",        "0770000002", "seller123").toRecord()
        );

        Files.write(dataFile, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // ==========================================================
    //  PRIVATE HELPERS
    // ==========================================================

    /**
     * Rewrites the entire users.txt from the in-memory list.
     * Called after every Create / Update / Delete operation.
     */
    private void saveAllUsers(List<AppUser> users) throws IOException {
        List<String> lines = new ArrayList<>();
        for (AppUser user : users) {
            lines.add(user.toRecord());
        }
        Files.write(dataFile, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /** Normalises an email address to lowercase and trimmed. */
    private String normalizeEmail(String email) {
        return AppUser.safe(email).trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Validates that an email is a properly formatted @gmail.com address.
     *
     * @return null if valid; an error message string if not
     */
    private String validateEmail(String email) {
        if (email.isBlank())
            return "Email address is required.";
        if (email.contains(" "))
            return "Email address cannot contain spaces.";
        if (email.chars().filter(ch -> ch == '@').count() != 1)
            return "Email must contain exactly one @ symbol.";
        if (!email.endsWith("@gmail.com"))
            return "Email must be a Gmail address (example@gmail.com).";
        String userPart = email.substring(0, email.indexOf('@'));
        if (userPart.isBlank())
            return "Enter the username part before @gmail.com.";
        if (!userPart.matches("[a-z0-9._]+"))
            return "Gmail username can only use letters, numbers, dots, and underscores.";
        if (!EMAIL_PATTERN.matcher(email).matches())
            return "Enter a valid Gmail address like example@gmail.com.";
        return null; // valid
    }

    /**
     * Validates that a phone number contains exactly 10 digits.
     *
     * @return null if valid; an error message string if not
     */
    private String validatePhone(String phone) {
        String digits = digitsOnly(phone);
        if (digits.isBlank())
            return "Enter your 10-digit mobile number.";
        if (!digits.equals(AppUser.safe(phone).trim()))
            return "Mobile number must contain digits only.";
        if (digits.length() != 10)
            return "Mobile number must be exactly 10 digits.";
        return null; // valid
    }

    /** Strips all non-digit characters from a string. */
    private String digitsOnly(String phone) {
        return AppUser.safe(phone).replaceAll("\\D", "");
    }

    /** Appends one line to the activity log file. */
    private void logActivity(String event, String detail) {
        try {
            String line = LocalDateTime.now()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    + "\t" + AppUser.safe(event)
                    + "\t" + AppUser.safe(detail);

            Files.writeString(LOGS_FILE, line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // logging failure should never crash the app
        }
    }
}
