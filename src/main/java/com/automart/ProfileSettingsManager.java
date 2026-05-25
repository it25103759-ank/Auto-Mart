package com.automart;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * ============================================================
 *  com.automart.ProfileSettingsManager.java
 *  Component 05 — com.automart.Profile Settings
 *  Student : Siriwardana A.K.L.B.  |  IT25103761
 * ============================================================
 *
 *  This class handles ALL CRUD operations for com.automart.Profile Settings:
 *
 *  CREATE  → createProfile()     — add a new profile to profiles.txt
 *  READ    → viewProfile()       — search and display a profile
 *  UPDATE  → updateUsername()    — change the username field
 *            updateAppearance()  — change the theme/appearance field
 *            updatePassword()    — change the password field
 *  DELETE  → deleteProfile()     — remove a profile from profiles.txt
 *
 *  OOP CONCEPTS USED
 *  -----------------
 *  Encapsulation  : Private helper methods hide file I/O details.
 *  Abstraction    : Public methods expose only what callers need.
 *  Composition    : Uses com.automart.Profile objects (has-a relationship).
 *  Single Responsibility : Each method does one thing only.
 *
 *  FILE  :  profiles.txt   (stored alongside other data files)
 *  FORMAT:  profileId,username,appearance,password
 * ============================================================
 */
public class ProfileSettingsManager {

    // ── File path ─────────────────────────────────────────────────────────

    /**
     * Path to the profiles data file.
     * Mirrors the data-directory convention used in AutoMartApplication.
     */
    private static final String PROFILES_FILE = "src/main/resources/data/profiles.txt";

    // Scanner is injected so the same instance can be reused across calls
    private final Scanner scanner;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Creates a com.automart.ProfileSettingsManager.
     *
     * @param scanner The shared Scanner (System.in) so we never open two
     *                Scanners on stdin at once — a common beginner bug.
     */
    public ProfileSettingsManager(Scanner scanner) {
        this.scanner = scanner;
        ensureFileExists(); // make sure profiles.txt exists on first run
    }


    // ══════════════════════════════════════════════════════════════════════
    //  CREATE — add a new profile
    // ══════════════════════════════════════════════════════════════════════

    /**
     * CREATE operation.
     *
     * Prompts the user for profile details, validates them, and appends
     * a new com.automart.Profile record to profiles.txt.
     *
     * Validation rules:
     *  - com.automart.Profile ID must be unique (no duplicates in profiles.txt)
     *  - Username and password cannot be blank
     *  - Appearance must be one of: DarkMode | LightMode | SystemDefault
     */
    public void createProfile() {
        printHeader("CREATE PROFILE SETTINGS");

        // ── Collect input ──────────────────────────────────────────────
        System.out.print("  Enter com.automart.Profile ID (e.g. P001) : ");
        String profileId = scanner.nextLine().trim();

        // Check for duplicate profile ID
        if (profileExists(profileId)) {
            printError("com.automart.Profile ID '" + profileId + "' already exists. Choose a different ID.");
            return;
        }

        System.out.print("  Enter Username               : ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            printError("Username cannot be empty.");
            return;
        }

        String appearance = chooseAppearance();

        System.out.print("  Enter Password               : ");
        String password = scanner.nextLine().trim();

        if (password.isEmpty()) {
            printError("Password cannot be empty.");
            return;
        }

        // ── Build and save ─────────────────────────────────────────────
        Profile profile = new Profile(profileId, username, appearance, password);

        try {
            // Append to profiles.txt (CREATE in file handling)
            Files.writeString(
                    Path.of(PROFILES_FILE),
                    profile.toRecord() + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            printSuccess("com.automart.Profile created successfully!");
            System.out.println(profile); // show what was saved
        } catch (IOException e) {
            printError("Could not write to profiles.txt: " + e.getMessage());
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    //  READ — view a profile
    // ══════════════════════════════════════════════════════════════════════

    /**
     * READ operation.
     *
     * Prompts for a com.automart.Profile ID, searches profiles.txt line-by-line, and
     * prints the matching profile using com.automart.Profile.toString().
     */
    public void viewProfile() {
        printHeader("VIEW PROFILE INFORMATION");

        System.out.print("  Enter com.automart.Profile ID to search   : ");
        String profileId = scanner.nextLine().trim();

        Profile profile = findProfileById(profileId);

        if (profile == null) {
            printError("No profile found with ID '" + profileId + "'.");
        } else {
            printSuccess("com.automart.Profile found!");
            System.out.println(profile);
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    //  UPDATE — three separate update operations
    // ══════════════════════════════════════════════════════════════════════

    /**
     * UPDATE — Username.
     *
     * Finds the profile by ID and replaces the username field, then
     * rewrites the entire profiles.txt file with the updated record.
     */
    public void updateUsername() {
        printHeader("UPDATE USERNAME");

        System.out.print("  Enter com.automart.Profile ID             : ");
        String profileId = scanner.nextLine().trim();

        Profile profile = findProfileById(profileId);
        if (profile == null) {
            printError("com.automart.Profile ID '" + profileId + "' not found.");
            return;
        }

        System.out.println("  Current Username             : " + profile.getUsername());
        System.out.print("  Enter New Username           : ");
        String newUsername = scanner.nextLine().trim();

        if (newUsername.isEmpty()) {
            printError("Username cannot be empty.");
            return;
        }

        // Apply the change and persist
        profile.setUsername(newUsername);
        saveAllProfiles(replaceProfile(profile));
        printSuccess("Username updated to '" + newUsername + "' successfully!");
    }

    /**
     * UPDATE — Appearance / Theme.
     *
     * Lets the user pick a new theme from a numbered list, then persists
     * the change to profiles.txt.
     */
    public void updateAppearance() {
        printHeader("UPDATE APPEARANCE SETTINGS");

        System.out.print("  Enter com.automart.Profile ID             : ");
        String profileId = scanner.nextLine().trim();

        Profile profile = findProfileById(profileId);
        if (profile == null) {
            printError("com.automart.Profile ID '" + profileId + "' not found.");
            return;
        }

        System.out.println("  Current Appearance           : " + profile.getAppearance());
        String newAppearance = chooseAppearance();

        profile.setAppearance(newAppearance);
        saveAllProfiles(replaceProfile(profile));
        printSuccess("Appearance updated to '" + newAppearance + "' successfully!");
    }

    /**
     * UPDATE — Password.
     *
     * Verifies the current password before allowing a change (basic
     * authentication check), then persists the new password.
     */
    public void updatePassword() {
        printHeader("UPDATE PASSWORD");

        System.out.print("  Enter com.automart.Profile ID             : ");
        String profileId = scanner.nextLine().trim();

        Profile profile = findProfileById(profileId);
        if (profile == null) {
            printError("com.automart.Profile ID '" + profileId + "' not found.");
            return;
        }

        // Verify current password before allowing change
        System.out.print("  Enter Current Password       : ");
        String currentPassword = scanner.nextLine().trim();

        if (!profile.getPassword().equals(currentPassword)) {
            printError("Incorrect current password. Update cancelled.");
            return;
        }

        System.out.print("  Enter New Password           : ");
        String newPassword = scanner.nextLine().trim();

        if (newPassword.isEmpty()) {
            printError("Password cannot be empty.");
            return;
        }

        System.out.print("  Confirm New Password         : ");
        String confirmPassword = scanner.nextLine().trim();

        if (!newPassword.equals(confirmPassword)) {
            printError("Passwords do not match. Update cancelled.");
            return;
        }

        profile.setPassword(newPassword);
        saveAllProfiles(replaceProfile(profile));
        printSuccess("Password updated successfully!");
    }


    // ══════════════════════════════════════════════════════════════════════
    //  DELETE — remove a profile
    // ══════════════════════════════════════════════════════════════════════

    /**
     * DELETE operation.
     *
     * Asks for a com.automart.Profile ID and, after confirmation, removes that record
     * from profiles.txt by rewriting the file without the deleted line.
     */
    public void deleteProfile() {
        printHeader("DELETE PROFILE");

        System.out.print("  Enter com.automart.Profile ID to delete   : ");
        String profileId = scanner.nextLine().trim();

        Profile profile = findProfileById(profileId);
        if (profile == null) {
            printError("com.automart.Profile ID '" + profileId + "' not found.");
            return;
        }

        // Show the profile before asking for confirmation
        System.out.println("\n  com.automart.Profile to be deleted:");
        System.out.println(profile);

        System.out.print("\n  ⚠  Are you sure you want to permanently delete this profile? (yes/no) : ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("yes")) {
            System.out.println("\n  [ Deletion cancelled. No changes made. ]\n");
            return;
        }

        // Remove the profile from the list and rewrite the file
        List<Profile> allProfiles = loadAllProfiles();
        allProfiles.removeIf(p -> p.getProfileId().equalsIgnoreCase(profileId));
        saveAllProfiles(allProfiles);

        printSuccess("com.automart.Profile '" + profileId + "' deleted successfully!");
    }


    // ══════════════════════════════════════════════════════════════════════
    //  Private helpers — file I/O and utility methods
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Reads all profiles from profiles.txt into a List.
     * Skips blank lines and malformed records.
     *
     * @return List of all valid com.automart.Profile objects
     */
    private List<Profile> loadAllProfiles() {
        List<Profile> profiles = new ArrayList<>();
        Path path = Path.of(PROFILES_FILE);

        if (!Files.exists(path)) {
            return profiles; // file doesn't exist yet — return empty list
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.isBlank()) continue;           // skip empty lines
                Profile p = Profile.fromRecord(line);
                if (p != null) profiles.add(p);         // skip malformed lines
            }
        } catch (IOException e) {
            printError("Error reading profiles.txt: " + e.getMessage());
        }

        return profiles;
    }

    /**
     * Overwrites profiles.txt with the given list of profiles.
     * This is the standard "read-modify-write" pattern for file-based updates.
     *
     * @param profiles The complete, up-to-date list of profiles to persist
     */
    private void saveAllProfiles(List<Profile> profiles) {
        List<String> lines = new ArrayList<>();
        for (Profile p : profiles) {
            lines.add(p.toRecord());
        }
        try {
            Files.write(
                    Path.of(PROFILES_FILE),
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING // wipe old content
            );
        } catch (IOException e) {
            printError("Error writing profiles.txt: " + e.getMessage());
        }
    }

    /**
     * Finds a com.automart.Profile by its profile ID (case-insensitive).
     *
     * @param profileId The ID to search for
     * @return The matching com.automart.Profile, or null if not found
     */
    private Profile findProfileById(String profileId) {
        for (Profile p : loadAllProfiles()) {
            if (p.getProfileId().equalsIgnoreCase(profileId)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Returns true if a profile with the given ID already exists in the file.
     *
     * @param profileId The ID to check
     * @return true if a duplicate exists, false otherwise
     */
    private boolean profileExists(String profileId) {
        return findProfileById(profileId) != null;
    }

    /**
     * Returns the full profile list with the given profile replacing the old
     * record that shares the same profile ID.
     *
     * @param updated The modified com.automart.Profile object
     * @return The updated list (ready to save)
     */
    private List<Profile> replaceProfile(Profile updated) {
        List<Profile> allProfiles = loadAllProfiles();
        for (int i = 0; i < allProfiles.size(); i++) {
            if (allProfiles.get(i).getProfileId().equalsIgnoreCase(updated.getProfileId())) {
                allProfiles.set(i, updated); // replace in-place
                break;
            }
        }
        return allProfiles;
    }

    /**
     * Presents a numbered menu for theme/appearance selection and returns
     * the chosen string value.
     *
     * @return One of "DarkMode" | "LightMode" | "SystemDefault"
     */
    private String chooseAppearance() {
        System.out.println("\n  Choose Appearance / Theme:");
        System.out.println("    1. DarkMode");
        System.out.println("    2. LightMode");
        System.out.println("    3. SystemDefault");
        System.out.print("  Enter choice (1-3)           : ");

        String choice = scanner.nextLine().trim();
        return switch (choice) {
            case "1" -> "DarkMode";
            case "2" -> "LightMode";
            case "3" -> "SystemDefault";
            default  -> {
                System.out.println("  [ Invalid choice — defaulting to DarkMode ]");
                yield "DarkMode";
            }
        };
    }

    /**
     * Creates the profiles.txt file (and its parent directories) if they
     * do not already exist. Called once in the constructor.
     */
    private void ensureFileExists() {
        Path path = Path.of(PROFILES_FILE);
        try {
            Files.createDirectories(path.getParent()); // create data/ folder if needed
            if (!Files.exists(path)) {
                Files.createFile(path);                // create empty profiles.txt
            }
        } catch (IOException e) {
            System.err.println("[com.automart.ProfileSettingsManager] Could not initialize profiles.txt: " + e.getMessage());
        }
    }


    // ── Console formatting helpers ────────────────────────────────────────

    /** Prints a section header banner. */
    private void printHeader(String title) {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.printf( "║  %-36s║%n", title);
        System.out.println("╚══════════════════════════════════════╝");
    }

    /** Prints a green-style success message. */
    private void printSuccess(String msg) {
        System.out.println("\n  ✔  " + msg + "\n");
    }

    /** Prints a red-style error message. */
    private void printError(String msg) {
        System.out.println("\n  ✘  ERROR: " + msg + "\n");
    }
}
