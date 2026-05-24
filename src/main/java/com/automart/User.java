package com.automart;

/**
 * User - Data model for a registered AutoMart user.
 * Stored in: data/users.txt
 * Format   : userId|username|email|password|phone|role|status
 */
public class User {

    private String userId;
    private String username;
    private String email;
    private String password;
    private String phone;
    private String role;    // buyer | seller | admin
    private String status;  // active | inactive

    // -------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------
    public User(String userId, String username, String email,
                String password, String phone, String role, String status) {
        this.userId   = userId;
        this.username = username;
        this.email    = email;
        this.password = password;
        this.phone    = phone;
        this.role     = role;
        this.status   = status;
    }

    // -------------------------------------------------------
    //  Getters
    // -------------------------------------------------------
    public String getUserId()   { return userId;   }
    public String getUsername() { return username; }
    public String getEmail()    { return email;    }
    public String getPassword() { return password; }
    public String getPhone()    { return phone;    }
    public String getRole()     { return role;     }
    public String getStatus()   { return status;   }

    // -------------------------------------------------------
    //  Setters (only mutable fields)
    // -------------------------------------------------------
    public void setEmail(String email)       { this.email    = email;    }
    public void setPassword(String password) { this.password = password; }
    public void setPhone(String phone)       { this.phone    = phone;    }
    public void setStatus(String status)     { this.status   = status;   }
    public void setRole(String role)         { this.role     = role;     }

    // -------------------------------------------------------
    //  File serialisation
    // -------------------------------------------------------

    /** Converts the user object to a pipe-delimited string for file storage. */
    public String toFileString() {
        return userId   + "|" + username + "|" + email    + "|" +
               password + "|" + phone    + "|" + role     + "|" + status;
    }

    /** Parses a pipe-delimited line from users.txt into a User object. */
    public static User fromFileString(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] parts = line.split("\\|", -1);
        if (parts.length != 7) return null;
        return new User(parts[0].trim(), parts[1].trim(), parts[2].trim(),
                        parts[3].trim(), parts[4].trim(), parts[5].trim(), parts[6].trim());
    }

    // -------------------------------------------------------
    //  Display
    // -------------------------------------------------------
    @Override
    public String toString() {
        return "┌─────────────────────────────────┐\n" +
               "│  User ID  : " + userId   + "\n" +
               "│  Username : " + username + "\n" +
               "│  Email    : " + email    + "\n" +
               "│  Phone    : " + phone    + "\n" +
               "│  Role     : " + role     + "\n" +
               "│  Status   : " + status   + "\n" +
               "└─────────────────────────────────┘";
    }
}
