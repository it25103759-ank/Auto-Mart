package com.automart;

import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *  Admin Dashboard  (CRUD Methods)
 * ---------------------------------------------------------------
 * Admin manages users, car listings, and requests.
 * Admins can view and moderate all platform data through the /admin panel.
 *
 * Route mappings (defined in AppHandler.java):
 *
 * ┌──────────────────────────┬────────────────────────────────────────┐
 * │ HTTP Endpoint            │ CRUD Operation                         │
 * ├──────────────────────────┼────────────────────────────────────────┤
 * │ GET  /admin              │ READ  - admin dashboard overview       │
 * │ GET  /admin/users.txt    │ READ  - download full users list       │
 * │ POST /admin/users/update │ UPDATE - edit a specific user's info   │
 * │ POST /admin/users/status │ UPDATE - ban / unban a user            │
 * │ POST /vehicle/delete     │ DELETE - remove any car listing        │
 * │ POST /vehicle/edit       │ UPDATE - edit any car listing          │
 * │ POST /requests/action    │ UPDATE - approve/reject/cancel request │
 * │ POST /reviews/action     │ DELETE - remove inappropriate review   │
 * └──────────────────────────┴────────────────────────────────────────┘
 *
 * Admin access is enforced via currentUser(exchange).isAdmin() guard in AppHandler.
 */
final class AdminCrud {

    // --- File paths ---
    private static final String USERS_FILE    = "users.txt";
    private static final String CARS_FILE     = "cars.txt";
    private static final String REQUESTS_FILE = "requests.txt";
    private static final String REVIEWS_FILE  = "reviews.txt";
    private static final String LOG_FILE      = "activity_log.txt";

    // --- Date formatter for activity logging ---
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------
    public AdminCrud() {
        // Ensure all data files exist on startup
        ensureFileExists(USERS_FILE);
        ensureFileExists(CARS_FILE);
        ensureFileExists(REQUESTS_FILE);
        ensureFileExists(REVIEWS_FILE);
        ensureFileExists(LOG_FILE);
    }

    // ===============================================================
    // GET /admin  —  READ: Admin Dashboard Overview
    // ===============================================================

    /**
     * Returns a summary overview of the entire platform:
     * total users, car listings, pending requests, and reviews.
     * Mapped to: GET /admin
     */
    public void getDashboardOverview() {
        List<String> users    = readFile(USERS_FILE);
        List<String> cars     = readFile(CARS_FILE);
        List<String> requests = readFile(REQUESTS_FILE);
        List<String> reviews  = readFile(REVIEWS_FILE);

        long pendingCount = requests.stream()
                .filter(r -> r.endsWith(",Pending"))
                .count();

        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║       AUTOMART — ADMIN OVERVIEW      ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.printf( "║  Total Users         : %-13d ║%n", users.size());
        System.out.printf( "║  Total Car Listings  : %-13d ║%n", cars.size());
        System.out.printf( "║  Total Requests      : %-13d ║%n", requests.size());
        System.out.printf( "║  Pending Requests    : %-13d ║%n", pendingCount);
        System.out.printf( "║  Total Reviews       : %-13d ║%n", reviews.size());
        System.out.println("╚══════════════════════════════════════╝");
    }

    // ===============================================================
    // GET /admin/users.txt  —  READ: Download Full Users List
    // ===============================================================

    /**
     * Reads and displays all users from users.txt.
     * Simulates the GET /admin/users.txt endpoint.
     * Format: UserID, Name, Email, Phone, Status
     */
    public void getAllUsers() {
        System.out.println("\n========== ALL REGISTERED USERS ==========");
        List<String> lines = readFile(USERS_FILE);

        if (lines.isEmpty()) {
            System.out.println("  No users found.");
            return;
        }

        System.out.printf("  %-8s %-15s %-25s %-14s %-10s%n",
                "ID", "Name", "Email", "Phone", "Status");
        System.out.println("  " + "─".repeat(76));

        for (String line : lines) {
            String[] p = line.split(",");
            // Support both 4-field (no status) and 5-field (with status) formats
            String status = (p.length >= 5) ? p[4].trim() : "Active";
            System.out.printf("  %-8s %-15s %-25s %-14s %-10s%n",
                    p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(), status);
        }
        System.out.println("===========================================");
    }

    // ===============================================================
    // POST /admin/users/update  —  UPDATE: Edit a User's Info
    // ===============================================================

    /**
     * Updates a specific user's name, email, or phone by their User ID.
     * Simulates POST /admin/users/update
     *
     * @param userId   - Target user ID (e.g., U001)
     * @param newName  - Updated name  (pass null to keep existing)
     * @param newEmail - Updated email (pass null to keep existing)
     * @param newPhone - Updated phone (pass null to keep existing)
     */
    public void updateUser(String userId, String newName, String newEmail, String newPhone) {
        List<String> lines = readFile(USERS_FILE);
        List<String> updated = new ArrayList<>();
        boolean found = false;

        for (String line : lines) {
            String[] p = line.split(",");
            if (p[0].trim().equalsIgnoreCase(userId)) {
                found = true;
                // Replace only the fields that were provided
                String name   = (newName  != null && !newName.isEmpty())  ? newName  : p[1].trim();
                String email  = (newEmail != null && !newEmail.isEmpty())  ? newEmail : p[2].trim();
                String phone  = (newPhone != null && !newPhone.isEmpty())  ? newPhone : p[3].trim();
                String status = (p.length >= 5) ? p[4].trim() : "Active";
                updated.add(p[0].trim() + "," + name + "," + email + "," + phone + "," + status);
                System.out.println("\n  ✔ User '" + userId + "' updated successfully.");
            } else {
                updated.add(line);
            }
        }

        if (!found) {
            System.out.println("\n  ✘ User ID '" + userId + "' not found.");
            return;
        }

        writeFile(USERS_FILE, updated);
        logActivity("ADMIN", "UPDATE USER", "Updated info for user: " + userId);
    }

    // ===============================================================
    // POST /admin/users/status  —  UPDATE: Ban / Unban a User
    // ===============================================================

    /**
     * Toggles a user's status between "Active" and "Banned".
     * Simulates POST /admin/users/status
     *
     * @param userId - The user to ban or unban
     * @param ban    - true to ban, false to unban
     */
    public void setUserStatus(String userId, boolean ban) {
        List<String> lines = readFile(USERS_FILE);
        List<String> updated = new ArrayList<>();
        boolean found = false;
        String newStatus = ban ? "Banned" : "Active";

        for (String line : lines) {
            String[] p = line.split(",");
            if (p[0].trim().equalsIgnoreCase(userId)) {
                found = true;
                // Rebuild line with updated status (5th field)
                String base = p[0].trim() + "," + p[1].trim() + "," +
                              p[2].trim() + "," + p[3].trim();
                updated.add(base + "," + newStatus);
                System.out.println("\n  ✔ User '" + userId + "' is now " + newStatus + ".");
            } else {
                updated.add(line);
            }
        }

        if (!found) {
            System.out.println("\n  ✘ User ID '" + userId + "' not found.");
            return;
        }

        writeFile(USERS_FILE, updated);
        logActivity("ADMIN", "USER STATUS", userId + " marked as " + newStatus);
    }

    // ===============================================================
    // POST /vehicle/delete  —  DELETE: Remove Any Car Listing
    // ===============================================================

    /**
     * Deletes a car listing from cars.txt by Car ID.
     * Simulates POST /vehicle/delete
     *
     * @param carId - The ID of the car to remove (e.g., C001)
     */
    public void deleteCar(String carId) {
        boolean deleted = deleteRecordById(CARS_FILE, carId);

        if (deleted) {
            System.out.println("\n  ✔ Car listing '" + carId + "' deleted successfully.");
            logActivity("ADMIN", "DELETE CAR", "Removed car listing: " + carId);
        } else {
            System.out.println("\n  ✘ Car ID '" + carId + "' not found.");
        }
    }

    // ===============================================================
    // POST /vehicle/edit  —  UPDATE: Edit Any Car Listing
    // ===============================================================

    /**
     * Updates a car listing's brand, model, or price by Car ID.
     * Simulates POST /vehicle/edit
     *
     * @param carId    - Target car ID (e.g., C002)
     * @param newBrand - Updated brand (pass null to keep existing)
     * @param newModel - Updated model (pass null to keep existing)
     * @param newPrice - Updated price (pass null to keep existing)
     */
    public void editCar(String carId, String newBrand, String newModel, String newPrice) {
        List<String> lines = readFile(CARS_FILE);
        List<String> updated = new ArrayList<>();
        boolean found = false;

        for (String line : lines) {
            String[] p = line.split(",");
            if (p[0].trim().equalsIgnoreCase(carId)) {
                found = true;
                String brand = (newBrand != null && !newBrand.isEmpty()) ? newBrand : p[1].trim();
                String model = (newModel != null && !newModel.isEmpty()) ? newModel : p[2].trim();
                String price = (newPrice != null && !newPrice.isEmpty()) ? newPrice : p[3].trim();
                updated.add(p[0].trim() + "," + brand + "," + model + "," + price);
                System.out.println("\n  ✔ Car '" + carId + "' updated successfully.");
            } else {
                updated.add(line);
            }
        }

        if (!found) {
            System.out.println("\n  ✘ Car ID '" + carId + "' not found.");
            return;
        }

        writeFile(CARS_FILE, updated);
        logActivity("ADMIN", "EDIT CAR", "Updated car listing: " + carId);
    }

    // ===============================================================
    // POST /requests/action  —  UPDATE: Approve / Reject / Cancel
    // ===============================================================

    /**
     * Updates the status of a purchase request.
     * Simulates POST /requests/action
     * Valid actions: "Approved", "Rejected", "Cancelled"
     *
     * @param requestId - The request to act on (e.g., R001)
     * @param action    - New status: "Approved", "Rejected", or "Cancelled"
     */
    public void handleRequestAction(String requestId, String action) {
        // Validate action
        List<String> validActions = Arrays.asList("Approved", "Rejected", "Cancelled");
        if (!validActions.contains(action)) {
            System.out.println("\n  ✘ Invalid action. Use: Approved, Rejected, or Cancelled.");
            return;
        }

        List<String> lines = readFile(REQUESTS_FILE);
        List<String> updated = new ArrayList<>();
        boolean found = false;

        for (String line : lines) {
            String[] p = line.split(",");
            if (p[0].trim().equalsIgnoreCase(requestId)) {
                found = true;
                // Rebuild with updated status (4th field)
                updated.add(p[0].trim() + "," + p[1].trim() + "," + p[2].trim() + "," + action);
                System.out.println("\n  ✔ Request '" + requestId + "' marked as: " + action);
            } else {
                updated.add(line);
            }
        }

        if (!found) {
            System.out.println("\n  ✘ Request ID '" + requestId + "' not found.");
            return;
        }

        writeFile(REQUESTS_FILE, updated);
        logActivity("ADMIN", "REQUEST ACTION", requestId + " → " + action);
    }

    // ===============================================================
    // POST /reviews/action  —  DELETE: Remove Inappropriate Review
    // ===============================================================

    /**
     * Removes a review from reviews.txt by Review ID.
     * Simulates POST /reviews/action
     * Format of reviews.txt: ReviewID,UserID,CarID,Rating,Comment
     *
     * @param reviewId - The review to delete (e.g., RV001)
     */
    public void deleteReview(String reviewId) {
        boolean deleted = deleteRecordById(REVIEWS_FILE, reviewId);

        if (deleted) {
            System.out.println("\n  ✔ Review '" + reviewId + "' removed successfully.");
            logActivity("ADMIN", "DELETE REVIEW", "Removed review: " + reviewId);
        } else {
            System.out.println("\n  ✘ Review ID '" + reviewId + "' not found.");
        }
    }

   
    /** Displays all car listings */
    public void getAllCars() {
        System.out.println("\n========== ALL CAR LISTINGS ==========");
        List<String> lines = readFile(CARS_FILE);

        if (lines.isEmpty()) {
            System.out.println("  No car listings found.");
            return;
        }

        System.out.printf("  %-8s %-12s %-15s %-15s%n", "Car ID", "Brand", "Model", "Price (LKR)");
        System.out.println("  " + "─".repeat(54));

        for (String line : lines) {
            String[] p = line.split(",");
            if (p.length >= 4) {
                System.out.printf("  %-8s %-12s %-15s %-15s%n",
                        p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim());
            }
        }
        System.out.println("=======================================");
    }

    /** Displays all purchase requests */
    public void getAllRequests() {
        System.out.println("\n========== PURCHASE REQUESTS ==========");
        List<String> lines = readFile(REQUESTS_FILE);

        if (lines.isEmpty()) {
            System.out.println("  No requests found.");
            return;
        }

        System.out.printf("  %-12s %-10s %-10s %-12s%n",
                "Request ID", "User ID", "Car ID", "Status");
        System.out.println("  " + "─".repeat(48));

        for (String line : lines) {
            String[] p = line.split(",");
            if (p.length >= 4) {
                System.out.printf("  %-12s %-10s %-10s %-12s%n",
                        p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim());
            }
        }
        System.out.println("========================================");
    }

    /** Displays all reviews */
    public void getAllReviews() {
        System.out.println("\n========== ALL REVIEWS ==========");
        List<String> lines = readFile(REVIEWS_FILE);

        if (lines.isEmpty()) {
            System.out.println("  No reviews found.");
            return;
        }

        System.out.printf("  %-8s %-8s %-8s %-6s %-30s%n",
                "Rev ID", "User ID", "Car ID", "Rating", "Comment");
        System.out.println("  " + "─".repeat(64));

        for (String line : lines) {
            String[] p = line.split(",", 5); // limit 5 so comment stays whole
            if (p.length >= 5) {
                System.out.printf("  %-8s %-8s %-8s %-6s %-30s%n",
                        p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(), p[4].trim());
            }
        }
        System.out.println("=================================");
    }

    // ===============================================================
    // ACTIVITY LOG HELPER
    // ===============================================================

    /**
     * Appends an action to activity_log.txt for audit trail.
     *
     * @param actor  - Who performed the action (e.g., "ADMIN")
     * @param action - What was done (e.g., "DELETE CAR")
     * @param detail - Details (e.g., "Removed car listing: C001")
     */
    public void logActivity(String actor, String action, String detail) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = timestamp + " | " + actor + " | " + action + " | " + detail;

        try (FileWriter fw = new FileWriter(LOG_FILE, true); // append mode
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logEntry);
        } catch (IOException e) {
            System.out.println("  [Warning] Could not write to activity log.");
        }
    }

    // ===============================================================
    // FILE HANDLING HELPERS (private)
    // ===============================================================

    /** Reads all non-empty lines from a file into a List */
    private List<String> readFile(String filename) {
        List<String> lines = new ArrayList<>();
        File file = new File(filename);
        if (!file.exists()) return lines;

        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (!line.isEmpty()) lines.add(line);
            }
        } catch (FileNotFoundException e) {
            System.out.println("  [Error] Cannot read: " + filename);
        }
        return lines;
    }

    /** Writes a list of lines to a file (overwrites) */
    private void writeFile(String filename, List<String> lines) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            for (String line : lines) pw.println(line);
        } catch (IOException e) {
            System.out.println("  [Error] Cannot write to: " + filename);
        }
    }

    /**
     * Deletes the first record whose first CSV field matches the given ID.
     * @return true if deleted, false if not found
     */
    private boolean deleteRecordById(String filename, String id) {
        List<String> lines = readFile(filename);
        List<String> updated = new ArrayList<>();
        boolean found = false;

        for (String line : lines) {
            if (line.split(",")[0].trim().equalsIgnoreCase(id)) {
                found = true; // skip this line → effectively deletes it
            } else {
                updated.add(line);
            }
        }

        if (found) writeFile(filename, updated);
        return found;
    }

    /** Creates a file if it does not already exist */
    private void ensureFileExists(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("  [Warning] Could not create file: " + filename);
            }
        }
    }
}
