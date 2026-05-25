/**
 * ============================================================
 *  Profile.java
 *  Component 05 — Profile Settings
 *  Student : Siriwardana A.K.L.B.  |  IT25103761
 * ============================================================
 *
 *  OOP CONCEPTS USED
 *  -----------------
 *  Encapsulation  : All fields are private; access only via getters/setters.
 *  Abstraction    : Callers work with Profile objects — they never touch
 *                   the raw file format directly.
 *  Composition    : A Profile "belongs to" a user (linked by profileId which
 *                   mirrors the username stored in AppUser).
 *
 *  FILE FORMAT  (profiles.txt, comma-separated)
 *  --------------------------------------------
 *  profileId , username , appearance , password
 *  Example:
 *  P001,Kasun,DarkMode,1234
 * ============================================================
 */
public class Profile {

    // ── Private fields (Encapsulation) ────────────────────────────────────

    /** Unique profile ID, e.g. "P001" */
    private String profileId;

    /** The username this profile belongs to (matches AppUser username) */
    private String username;

    /**
     * Appearance / theme preference.
     * Valid values: "DarkMode" | "LightMode" | "SystemDefault"
     */
    private String appearance;

    /**
     * Plain-text password stored for the profile settings record.
     * NOTE: In production you would store a hash (see AutoMartApplication.hashPassword).
     *       For this beginner-friendly component we keep it readable so students
     *       can verify the file contents easily.
     */
    private String password;


    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Creates a fully populated Profile object.
     *
     * @param profileId  Unique ID (e.g. "P001")
     * @param username   Owner's username
     * @param appearance Theme choice ("DarkMode" / "LightMode" / "SystemDefault")
     * @param password   Profile password
     */
    public Profile(String profileId, String username, String appearance, String password) {
        this.profileId  = profileId;
        this.username   = username;
        this.appearance = appearance;
        this.password   = password;
    }


    // ── Getters ───────────────────────────────────────────────────────────

    /** @return The unique profile ID */
    public String getProfileId() {
        return profileId;
    }

    /** @return The username that owns this profile */
    public String getUsername() {
        return username;
    }

    /** @return The current appearance / theme setting */
    public String getAppearance() {
        return appearance;
    }

    /** @return The profile password */
    public String getPassword() {
        return password;
    }


    // ── Setters ───────────────────────────────────────────────────────────

    /** @param profileId New profile ID */
    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    /** @param username New username */
    public void setUsername(String username) {
        this.username = username;
    }

    /** @param appearance New appearance/theme value */
    public void setAppearance(String appearance) {
        this.appearance = appearance;
    }

    /** @param password New password */
    public void setPassword(String password) {
        this.password = password;
    }


    // ── File serialization helpers ────────────────────────────────────────

    /**
     * Converts this Profile to a single comma-separated line suitable for
     * writing to profiles.txt.
     *
     * Format:  profileId,username,appearance,password
     *
     * @return The CSV line (no trailing newline)
     */
    public String toRecord() {
        return profileId + "," + username + "," + appearance + "," + password;
    }

    /**
     * Parses one line from profiles.txt and returns a Profile object.
     * Returns null if the line is malformed (wrong number of fields).
     *
     * @param line A raw line read from profiles.txt
     * @return     A Profile, or null if the line cannot be parsed
     */
    public static Profile fromRecord(String line) {
        // Split on commas — we expect exactly 4 fields
        String[] parts = line.split(",", -1);
        if (parts.length < 4) {
            return null; // malformed line — skip it
        }
        return new Profile(
                parts[0].trim(), // profileId
                parts[1].trim(), // username
                parts[2].trim(), // appearance
                parts[3].trim()  // password
        );
    }




    
    @Override
    public String toString() {
        return "┌─────────────────────────────────────┐\n"
             + "│          PROFILE INFORMATION        │\n"
             + "├─────────────────────────────────────┤\n"
             + "│  Profile ID  : " + pad(profileId, 21)  + " │\n"
             + "│  Username    : " + pad(username, 21)   + " │\n"
             + "│  Appearance  : " + pad(appearance, 21) + " │\n"
             + "│  Password    : " + pad(maskPassword(), 21) + " │\n"
             + "└─────────────────────────────────────┘";
    }

    /** Masks all but the first character of the password for display. */
    private String maskPassword() {
        if (password == null || password.isEmpty()) return "(none)";
        return password.charAt(0) + "*".repeat(password.length() - 1);
    }

    /** Right-pads a string to the given width for table formatting. */
    private String pad(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text.substring(0, width);
        return text + " ".repeat(width - text.length());
    }
}
