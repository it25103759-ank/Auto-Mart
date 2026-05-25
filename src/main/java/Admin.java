public class Admin {

    // --- Fields (Private for encapsulation) ---
    private String username;
    private String password;

    // -------------------------------------------------------
   
    // -------------------------------------------------------
    public Admin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // -------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // -------------------------------------------------------
    // Method: Authenticate - checks entered credentials
    // Returns true if both username and password match
    // -------------------------------------------------------
    public boolean authenticate(String enteredUsername, String enteredPassword) {
        return this.username.equals(enteredUsername) && this.password.equals(enteredPassword);
    }

    // -------------------------------------------------------
    // toString: Useful for debugging
    // -------------------------------------------------------
    @Override
    public String toString() {
        return "Admin{username='" + username + "'}";
    }
}
