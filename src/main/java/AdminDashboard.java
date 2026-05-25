import java.io.*;
import java.util.*;

public class AdminDashboard {

    // --- File paths for data storage ---
    private static final String USERS_FILE    = "users.txt";
    private static final String CARS_FILE     = "cars.txt";
    private static final String REQUESTS_FILE = "requests.txt";

    // -------------------------------------------------------
    // Constructor (default)
    // -------------------------------------------------------
    public AdminDashboard() {
        // No initial setup needed; files are accessed on demand
    }

    // ===============================================================
    //  DISPLAY MENU
    // ===============================================================

    /**
     * Displays the Admin Dashboard main menu in the console.
     */
    public void displayMenu() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║        AUTOMART - ADMIN DASHBOARD    ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  1. View Users                       ║");
        System.out.println("║  2. View Car Listings                ║");
        System.out.println("║  3. View Requests                    ║");
        System.out.println("║  4. Delete User                      ║");
        System.out.println("║  5. Delete Car Listing               ║");
        System.out.println("║  6. Exit                             ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Enter your choice: ");
    }

    // ===============================================================
    //  MANAGE USERS
    // ===============================================================

    /**

     * Reads and displays all users from users.txt
     * Format: UserID, Name, Email, Phone

     */
    public void viewUsers() {
        System.out.println("\n========== REGISTERED USERS ==========");
        List<String> lines = readFile(USERS_FILE);

        if (lines.isEmpty()) {
            System.out.println("  No users found.");
        } else {
            // Print column headers
            System.out.printf("  %-8s %-15s %-25s %-15s%n",
                    "User ID", "Name", "Email", "Phone");
            System.out.println("  " + "-".repeat(65));

            // Print each user record
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    System.out.printf("  %-8s %-15s %-25s %-15s%n",
                            parts[0].trim(),  // User ID
                            parts[1].trim(),  // Name
                            parts[2].trim(),  // Email
                            parts[3].trim()); // Phone
                } else {
                    // Handle malformed lines gracefully
                    System.out.println("  [Invalid record]: " + line);
                }
            }
        }
        System.out.println("=======================================");
    }

    /**
     * Deletes a user from users.txt by their User ID.
     * @param userId - The ID of the user to delete (e.g., U001)
     */
    public void deleteUser(String userId) {
        boolean deleted = deleteRecordById(USERS_FILE, userId);

        if (deleted) {
            System.out.println("\n  ✔ User '" + userId + "' deleted successfully.");
        } else {
            System.out.println("\n  ✘ User ID '" + userId + "' not found.");
        }
    }

    // ===============================================================
    //  MANAGE CAR LISTINGS
    // ===============================================================

    /**
     * Reads and displays all car listings from cars.txt
     * Format: CarID, Brand, Model, Price
     * Example line: C001,Toyota,Prius,6500000
     */
    public void viewCarListings() {
        System.out.println("\n========== CAR LISTINGS ==========");
        List<String> lines = readFile(CARS_FILE);

        if (lines.isEmpty()) {
            System.out.println("  No car listings found.");
        } else {
            // Print column headers
            System.out.printf("  %-8s %-12s %-15s %-15s%n",
                    "Car ID", "Brand", "Model", "Price (LKR)");
            System.out.println("  " + "-".repeat(55));

            // Print each car record
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    System.out.printf("  %-8s %-12s %-15s %-15s%n",
                            parts[0].trim(),  // Car ID
                            parts[1].trim(),  // Brand
                            parts[2].trim(),  // Model
                            parts[3].trim()); // Price
                } else {
                    System.out.println("  [Invalid record]: " + line);
                }
            }
        }
        System.out.println("===================================");
    }

    /**
     * Deletes a car listing from cars.txt by Car ID.
     * @param carId - The ID of the car to delete (e.g., C001)
     */
    public void deleteCarListing(String carId) {
        boolean deleted = deleteRecordById(CARS_FILE, carId);

        if (deleted) {
            System.out.println("\n  ✔ Car listing '" + carId + "' deleted successfully.");
        } else {
            System.out.println("\n  ✘ Car ID '" + carId + "' not found.");
        }
    }

    // ===============================================================
    //  MONITOR REQUESTS
    // ===============================================================

    /**
     * Reads and displays all purchase requests from requests.txt
     * Format: RequestID, UserID, CarID, Status
     * Example line: R001,U001,C001,Pending
     */
    public void viewRequests() {
        System.out.println("\n========== PURCHASE REQUESTS ==========");
        List<String> lines = readFile(REQUESTS_FILE);

        if (lines.isEmpty()) {
            System.out.println("  No requests found.");
        } else {
            // Print column headers
            System.out.printf("  %-12s %-10s %-10s %-12s%n",
                    "Request ID", "User ID", "Car ID", "Status");
            System.out.println("  " + "-".repeat(48));

            // Print each request record
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    System.out.printf("  %-12s %-10s %-10s %-12s%n",
                            parts[0].trim(),  // Request ID
                            parts[1].trim(),  // User ID
                            parts[2].trim(),  // Car ID
                            parts[3].trim()); // Status
                } else {
                    System.out.println("  [Invalid record]: " + line);
                }
            }
        }
        System.out.println("=======================================");
    }

    // ===============================================================
    //  FILE HANDLING HELPER METHODS
    // ===============================================================

    /**
     * Reads all non-empty lines from a given file.
     * @param filename - The name/path of the file to read
     * @return A List of Strings, each representing one line
     */
    private List<String> readFile(String filename) {
        List<String> lines = new ArrayList<>();
        File file = new File(filename);

        // Check if the file exists before reading
        if (!file.exists()) {
            System.out.println("  [Warning] File not found: " + filename);
            return lines;
        }

        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                // Skip empty lines
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("  [Error] Could not read file: " + filename);
        }

        return lines;
    }

    /**
     * Deletes a record from a file by matching the first field (ID).
     * Rewrites the file without the deleted line.
     * @param filename - File to modify
     * @param id       - The record ID to delete (e.g., U001, C002)
     * @return true if a record was deleted, false if not found
     */
    private boolean deleteRecordById(String filename, String id) {
        List<String> lines = readFile(filename);
        List<String> updatedLines = new ArrayList<>();
        boolean found = false;

        // Filter out the line that starts with the given ID
        for (String line : lines) {
            String firstField = line.split(",")[0].trim();
            if (firstField.equalsIgnoreCase(id)) {
                found = true; // Mark as found; skip this line (delete it)
            } else {
                updatedLines.add(line); // Keep all other lines
            }
        }

        // Only rewrite the file if the record was found and removed
        if (found) {
            writeFile(filename, updatedLines);
        }

        return found;
    }

    /**
     * Writes a list of lines back to a file (overwrites the file).
     * @param filename - The file to write to
     * @param lines    - The lines to write
     */
    private void writeFile(String filename, List<String> lines) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (String line : lines) {
                writer.println(line);
            }
        } catch (IOException e) {
            System.out.println("  [Error] Could not write to file: " + filename);
        }
    }
}
