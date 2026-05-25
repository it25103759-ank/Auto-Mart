import java.util.Scanner;

public class AutoMartApplication {

    // Maximum login attempts before locking out
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    public static void main(String[] args) {

        // --- Create Scanner for all user input ---
        Scanner scanner = new Scanner(System.in);

        // --- Display Application Banner ---
        printBanner();

        // --- Create Admin object with default credentials ---
        // Default username: admin | Default password: 1234
        Admin admin = new Admin("admin", "1234");

        // --- Handle Admin Login ---
        boolean loginSuccessful = handleLogin(admin, scanner);

        if (!loginSuccessful) {
            System.out.println("\n  ✘ Too many failed attempts. Access denied. Exiting...\n");
            scanner.close();
            return; // Exit the application
        }

        // --- Login Successful: Launch Dashboard ---
        System.out.println("\n  ✔ Login successful! Welcome, " + admin.getUsername() + "!");

        AdminDashboard dashboard = new AdminDashboard();

        // --- Main Menu Loop ---
        runDashboard(dashboard, scanner);

        // --- Cleanup ---
        System.out.println("\n  Thank you for using AutoMart Admin Panel. Goodbye!\n");
        scanner.close();
    }

    // ===============================================================
    //  PRINT APPLICATION BANNER
    // ===============================================================

    /**
     * Prints the welcome banner at application startup.
     */
    private static void printBanner() {
        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║        AUTOMART - VEHICLE MARKETPLACE    ║");
        System.out.println("║           Admin Management System        ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();
    }

    // ===============================================================
    //  HANDLE ADMIN LOGIN
    // ===============================================================

    /**
     *
     * @param admin   - The Admin object holding valid credentials
     * @param scanner - Scanner for reading console input
     * @return true if login succeeded, false after too many failures
     */
    private static boolean handleLogin(Admin admin, Scanner scanner) {
        System.out.println("  Please log in to access the Admin Dashboard.");
        System.out.println("  ─────────────────────────────────────────");

        int attempts = 0;

        while (attempts < MAX_LOGIN_ATTEMPTS) {
            // Prompt for username
            System.out.print("\n  Username: ");
            String enteredUsername = scanner.nextLine().trim();

            // Prompt for password
            System.out.print("  Password: ");
            String enteredPassword = scanner.nextLine().trim();

            // Authenticate credentials using Admin class method
            if (admin.authenticate(enteredUsername, enteredPassword)) {
                return true; // Login successful
            } else {
                attempts++;
                int remaining = MAX_LOGIN_ATTEMPTS - attempts;
                System.out.println("  ✘ Invalid credentials. "
                        + (remaining > 0 ? remaining + " attempt(s) remaining." : ""));
            }
        }

        return false; // Exceeded max attempts
    }

    // ===============================================================
    //  RUN THE ADMIN DASHBOARD (Menu Loop)
    // ===============================================================

    /**
     * Displays the dashboard menu repeatedly until admin chooses to exit.
     * Routes each choice to the appropriate AdminDashboard method.
     *
     * @param dashboard - The AdminDashboard instance
     * @param scanner   - Scanner for reading console input
     */
    private static void runDashboard(AdminDashboard dashboard, Scanner scanner) {
        boolean running = true;

        while (running) {
            // Display the menu
            dashboard.displayMenu();

            String input = scanner.nextLine().trim();

            // Parse the input safely
            int choice = parseChoice(input);

            // Route to the correct function based on user choice
            switch (choice) {

                case 1:
                    // View all registered users
                    dashboard.viewUsers();
                    break;

                case 2:
                    // View all car listings
                    dashboard.viewCarListings();
                    break;

                case 3:
                    // View all purchase requests
                    dashboard.viewRequests();
                    break;

                case 4:
                    // Delete a user by ID
                    System.out.print("\n  Enter User ID to delete (e.g., U001): ");
                    String userId = scanner.nextLine().trim();
                    if (!userId.isEmpty()) {
                        dashboard.deleteUser(userId);
                    } else {
                        System.out.println("  ✘ User ID cannot be empty.");
                    }
                    break;

                case 5:
                    // Delete a car listing by ID
                    System.out.print("\n  Enter Car ID to delete (e.g., C001): ");
                    String carId = scanner.nextLine().trim();
                    if (!carId.isEmpty()) {
                        dashboard.deleteCarListing(carId);
                    } else {
                        System.out.println("  ✘ Car ID cannot be empty.");
                    }
                    break;

                case 6:
                    // Exit the dashboard
                    System.out.println("\n  Exiting Admin Dashboard...");
                    running = false;
                    break;

                default:
                    // Handle invalid input
                    System.out.println("\n  ✘ Invalid choice. Please enter a number between 1 and 6.");
                    break;
            }

            // Pause briefly before showing the menu again (better UX)
            if (running) {
                System.out.println("\n  Press ENTER to continue...");
                scanner.nextLine();
            }
        }
    }

    // ===============================================================
    //  UTILITY: Parse integer from string safely
    // ===============================================================

    /**
     * Safely parses an integer from a String input.
     * Returns -1 if the input is not a valid integer.
     *
     * @param input - Raw string from console
     * @return Parsed integer or -1 on failure
     */
    private static int parseChoice(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1; // Signal invalid input
        }
    }
}
