import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static com.automart.AutoMartApplication.*;

/**
 * COMPONENT 05 – Admin Dashboard  (CRUD Methods)
 * ---------------------------------------------------------------
 * Admin manages users, car listings, and requests.
 * Admins can view and moderate all platform data through the /admin panel.
 *
 *  Route mappings (defined in AppHandler.java):
 *  ┌──────────────────────────────────┬────────────────────────────────────────┐
 *  │ HTTP Endpoint                    │ CRUD Operation                         │
 *  ├──────────────────────────────────┼────────────────────────────────────────┤
 *  │ GET  /admin                      │ READ  – admin dashboard overview       │
 *  │ GET  /admin/users.txt            │ READ  – download full users list       │
 *  │ POST /admin/users/update         │ UPDATE – edit a specific user's info   │
 *  │ POST /admin/users/status         │ UPDATE – ban / unban a user            │
 *  │ POST /vehicle/delete             │ DELETE – remove any car listing        │
 *  │ POST /vehicle/edit               │ UPDATE – edit any car listing          │
 *  │ POST /requests/action            │ UPDATE – approve/reject/cancel request │
 *  │ POST /reviews/action             │ DELETE – remove inappropriate review   │
 *  └──────────────────────────────────┴────────────────────────────────────────┘
 *
 * Admin access is enforced via currentUser(exchange).isAdmin() guard in AppHandler.
 */
final class AdminCrud {

    // ── READ – Manage Users ───────────────────────────────────────────────────

    /**
     * Load all users for admin overview display.
     * Returns a full list from users.txt so admin can see every account.
     */
    static List<AppUser> loadAllUsers() throws IOException {
        List<AppUser> users = new ArrayList<>();
        if (!Files.exists(USERS_FILE)) return users;
        for (String line : Files.readAllLines(USERS_FILE, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            AppUser user = AppUser.fromRecord(line);
            if (user != null) users.add(user);
        }
        return users;
    }

    // ── READ – Manage Car Listings ────────────────────────────────────────────

    /**
     * Load all car listings from submissions.tsv for admin review.
     */
    static List<Vehicle> loadAllListings() throws IOException {
        return FileHelper.readVehicles();
    }

    // ── READ – Monitor System ─────────────────────────────────────────────────

    /**
     * Load activity log lines from logs.txt for admin monitoring.
     * Returns the most recent 200 lines.
     */
    static List<String> loadActivityLog() throws IOException {
        if (!Files.exists(LOGS_FILE)) return List.of();
        List<String> all = Files.readAllLines(LOGS_FILE, StandardCharsets.UTF_8);
        int from = Math.max(0, all.size() - 200);
        return all.subList(from, all.size());
    }

    /**
     * Return system metrics for the admin dashboard:
     * - Total users
     * - Total listings
     * - Pending requests
     * - Total reviews
     */
    static AdminMetrics getSystemMetrics() throws IOException {
        int totalUsers    = loadAllUsers().size();
        int totalListings = loadAllListings().size();
        int totalRequests = BuyingRequestCrud.loadRequests().size();
        int pendingRequests = (int) BuyingRequestCrud.loadRequests().stream()
                .filter(r -> "Pending".equalsIgnoreCase(r.status)).count();
        int totalReviews  = FeedbackReviewCrud.loadReviews().size();
        return new AdminMetrics(totalUsers, totalListings, totalRequests, pendingRequests, totalReviews);
    }

    // ── UPDATE – User Management ──────────────────────────────────────────────

    /**
     * Admin: update a specific user's email and phone.
     *
     * @param usernameOrEmail  the target user's username or email
     * @param newEmail         new email address (validated)
     * @param newPhone         new phone number (validated)
     * @return true if updated successfully
     */
    static boolean adminUpdateUserDetails(String usernameOrEmail,
                                           String newEmail,
                                           String newPhone) throws IOException {
        String target = safe(usernameOrEmail).trim();
        if (target.isBlank()) return false;

        List<AppUser> users = loadAllUsers();
        boolean updated = false;

        for (AppUser user : users) {
            if (user.getUsername().equalsIgnoreCase(target)
                    || user.getEmail().equalsIgnoreCase(target)) {

                String emailError = validateEmailAddress(normalizeEmailAddress(newEmail));
                if (emailError != null) return false;

                String phoneError = validatePhoneNumber(newPhone);
                if (phoneError != null) return false;

                user.setEmail(normalizeEmailAddress(newEmail));
                user.setPhone(digitsOnly(newPhone));
                updated = true;
                logActivity("ADMIN_USER_UPDATED",
                        "Admin updated details for " + user.getUsername());
                break;
            }
        }

        if (updated) {
            Files.write(USERS_FILE,
                    users.stream().map(AppUser::toRecord).toList(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }
        return updated;
    }

    /**
     * Admin: set a user's account status (active / banned) and update notes.
     *
     * @param usernameOrEmail  target user
     * @param status           "active" or "banned"
     * @param banReason        reason (shown to user if banned)
     * @param adminNote        private admin annotation
     * @return true if the user was found and updated
     */
    static boolean adminUpdateUserStatus(String usernameOrEmail,
                                          String status,
                                          String banReason,
                                          String adminNote) throws IOException {
        String target = safe(usernameOrEmail).trim();
        if (target.isBlank()) return false;

        List<AppUser> users = loadAllUsers();
        boolean updated = false;

        for (AppUser user : users) {
            if (user.getUsername().equalsIgnoreCase(target)
                    || user.getEmail().equalsIgnoreCase(target)) {
                user.setStatus(status);
                user.setBanReason(banReason);
                user.setAdminNote(adminNote);
                updated = true;
                logActivity("ADMIN_USER_STATUS",
                        "Admin set " + user.getUsername() + " to " + user.getStatus());
                break;
            }
        }

        if (updated) {
            Files.write(USERS_FILE,
                    users.stream().map(AppUser::toRecord).toList(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }
        return updated;
    }

    // ── DELETE – Listing Removal ──────────────────────────────────────────────

    /**
     * Admin: force-delete any car listing by ID (no ownership check).
     */
    static boolean adminDeleteListing(String id) throws IOException {
        List<Vehicle> vehicles = FileHelper.readVehicles();
        int before = vehicles.size();
        vehicles.removeIf(v -> v.id.equals(id));
        boolean removed = vehicles.size() < before;
        if (removed) {
            FileHelper.writeVehicles(vehicles);
            logActivity("ADMIN_LISTING_DELETED", "Admin deleted listing [" + id + "]");
        }
        return removed;
    }

    // ── Inner record for metrics ──────────────────────────────────────────────

    record AdminMetrics(int totalUsers, int totalListings,
                        int totalRequests, int pendingRequests,
                        int totalReviews) {}
}
