// ============================================================
//  RequestManager.java
//  AutoMart Application — Buying & Request Management
//  Handles all CRUD operations and file I/O for requests.
// ============================================================

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * RequestManager manages a list of Request objects in memory
 * and keeps them in sync with the requests.txt file on disk.
 *
 * OOP concepts used:
 *   - Inheritance   : extends BaseManager for the shared dataFile field
 *   - Encapsulation : the requests list is private; manipulated via methods
 *   - CRUD          : Create, Read, Update, Delete methods are clearly defined
 *   - File handling : reads and writes requests.txt using java.nio.file.Files
 */
public class RequestManager extends BaseManager {

    // ── Path to the data file ────────────────────────────────
    /** Default file path — you can pass a custom path to the constructor. */
    private static final Path DEFAULT_FILE =
            Paths.get("src/main/resources/data/requests.txt");

    // ── In-memory list of all requests ───────────────────────
    /** All requests loaded from the file; kept in sync on every change. */
    private final List<Request> requests = new ArrayList<>();

    // ── Constructor ──────────────────────────────────────────
    /**
     * Creates a RequestManager that uses the default requests.txt path.
     * Immediately loads any existing requests from the file.
     */
    public RequestManager() {
        super(DEFAULT_FILE);
        loadFromFile(); // Read existing requests when the manager starts
    }

    /**
     * Creates a RequestManager with a custom file path.
     * Useful for testing or alternate environments.
     *
     * @param filePath Custom path to the requests file
     */
    public RequestManager(Path filePath) {
        super(filePath);
        loadFromFile();
    }

    // ════════════════════════════════════════════════════════
    //  CREATE — Add a new purchase request
    // ════════════════════════════════════════════════════════
    /**
     * Creates a brand-new purchase request and saves it to the file.
     *
     * @param vehicleId     ID of the vehicle the buyer wants
     * @param vehicleTitle  Display name of the vehicle
     * @param buyerUsername Username of the buyer making the request
     * @param note          Optional note / message from the buyer
     * @return              The newly created Request object
     */
    public Request createRequest(String vehicleId, String vehicleTitle,
                                  String buyerUsername, String note) {

        // Use the static factory method on Request to build a new object
        Request newRequest = Request.create(vehicleId, vehicleTitle, buyerUsername, note);

        // Add it to the in-memory list
        requests.add(newRequest);

        // Append only the new line to the file (faster than rewriting everything)
        appendToFile(newRequest);

        System.out.println("\n✅ Request created successfully!");
        System.out.println(newRequest);

        return newRequest;
    }

    // ════════════════════════════════════════════════════════
    //  READ — View / search requests
    // ════════════════════════════════════════════════════════
    /**
     * Returns a copy of the full list of all requests in memory.
     *
     * @return List of all Request objects
     */
    public List<Request> getAllRequests() {
        return new ArrayList<>(requests); // return a copy so outside code can't modify our list
    }

    /**
     * Finds a single request by its unique ID.
     *
     * @param id The request ID to look for (e.g. "R001")
     * @return   The matching Request, or null if not found
     */
    public Request findById(String id) {
        // Linear search through the list
        for (Request r : requests) {
            if (r.getId().equalsIgnoreCase(id.trim())) {
                return r;
            }
        }
        return null; // Not found
    }

    /**
     * Returns all requests that belong to a specific buyer.
     *
     * @param buyerUsername The buyer's username
     * @return              List of requests made by that buyer
     */
    public List<Request> findByBuyer(String buyerUsername) {
        List<Request> results = new ArrayList<>();
        for (Request r : requests) {
            if (r.getBuyerUsername().equalsIgnoreCase(buyerUsername.trim())) {
                results.add(r);
            }
        }
        return results;
    }

    /**
     * Returns all requests that have a specific status.
     *
     * @param status One of: Pending, Approved, Rejected, Cancelled
     * @return       Matching requests
     */
    public List<Request> findByStatus(String status) {
        List<Request> results = new ArrayList<>();
        for (Request r : requests) {
            if (r.getStatus().equalsIgnoreCase(status.trim())) {
                results.add(r);
            }
        }
        return results;
    }

    /**
     * Prints all requests to the console in a readable format.
     * Shows a message if there are no requests.
     */
    public void printAllRequests() {
        if (requests.isEmpty()) {
            System.out.println("\n  (No requests found.)");
            return;
        }

        System.out.printf("%n  ── %d Request(s) Found ──%n%n", requests.size());
        for (Request r : requests) {
            System.out.println(r);   // calls Request.toString()
            System.out.println();     // blank line between requests
        }
    }

    /**
     * Prints only requests that match a given status.
     *
     * @param status The status to filter by
     */
    public void printByStatus(String status) {
        List<Request> filtered = findByStatus(status);
        if (filtered.isEmpty()) {
            System.out.println("\n  (No " + status + " requests found.)");
            return;
        }

        System.out.printf("%n  ── %d %s Request(s) ──%n%n", filtered.size(), status);
        for (Request r : filtered) {
            System.out.println(r);
            System.out.println();
        }
    }

    // ════════════════════════════════════════════════════════
    //  UPDATE — Change the status of a request
    // ════════════════════════════════════════════════════════
    /**
     * Updates the status of an existing request and saves the change to the file.
     *
     * @param id        The ID of the request to update
     * @param newStatus The new status value (Pending / Approved / Rejected / Cancelled)
     * @return          true if the request was found and updated; false otherwise
     */
    public boolean updateStatus(String id, String newStatus) {
        Request target = findById(id);

        if (target == null) {
            System.out.println("  ❌ Request not found: " + id);
            return false;
        }

        // Validate the new status value
        if (!isValidStatus(newStatus)) {
            System.out.println("  ❌ Invalid status: " + newStatus);
            System.out.println("     Valid options: Pending, Approved, Rejected, Cancelled");
            return false;
        }

        // Apply the change
        String oldStatus = target.getStatus();
        target.setStatus(newStatus);

        // Rewrite the entire file to reflect the updated list
        saveAllToFile();

        System.out.printf("%n  ✅ Request %s status changed: %s → %s%n",
                id, oldStatus, newStatus);
        return true;
    }

    /**
     * Approves a request (sets status to "Approved").
     *
     * @param id Request ID to approve
     * @return   true on success
     */
    public boolean approveRequest(String id) {
        return updateStatus(id, Request.STATUS_APPROVED);
    }

    /**
     * Rejects a request (sets status to "Rejected").
     *
     * @param id Request ID to reject
     * @return   true on success
     */
    public boolean rejectRequest(String id) {
        return updateStatus(id, Request.STATUS_REJECTED);
    }

    /**
     * Cancels a request (sets status to "Cancelled").
     * Typically called by the buyer who made the request.
     *
     * @param id Request ID to cancel
     * @return   true on success
     */
    public boolean cancelRequest(String id) {
        return updateStatus(id, Request.STATUS_CANCELLED);
    }

    // ════════════════════════════════════════════════════════
    //  DELETE — Remove a request permanently
    // ════════════════════════════════════════════════════════
    /**
     * Permanently deletes a request from the list and from the file.
     *
     * @param id The ID of the request to delete
     * @return   true if the request was found and deleted; false otherwise
     */
    public boolean deleteRequest(String id) {
        Request target = findById(id);

        if (target == null) {
            System.out.println("  ❌ Request not found: " + id);
            return false;
        }

        // Remove from the in-memory list
        requests.remove(target);

        // Rewrite the file without the deleted request
        saveAllToFile();

        System.out.println("\n  ✅ Request " + id + " deleted successfully.");
        return true;
    }

    // ════════════════════════════════════════════════════════
    //  FILE HANDLING — Read and write requests.txt
    // ════════════════════════════════════════════════════════
    /**
     * Loads all requests from the text file into the in-memory list.
     * Called once when the RequestManager is first created.
     *
     * File format (tab-separated):
     *   id \t vehicleId \t vehicleTitle \t buyerUsername \t note \t status \t createdAt
     */
    private void loadFromFile() {
        requests.clear(); // Start fresh

        // If the file doesn't exist yet, there's nothing to load
        if (!Files.exists(dataFile)) {
            System.out.println("  (No existing requests file found — starting fresh.)");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(dataFile, StandardCharsets.UTF_8);
            int maxNum = 0; // track highest request number to set the counter correctly

            for (String line : lines) {
                if (line.isBlank()) continue; // skip empty lines

                Request r = Request.fromRecord(line);
                if (r != null) {
                    requests.add(r);

                    // Extract the numeric part of the ID (e.g. "R007" → 7)
                    try {
                        String numPart = r.getId().replaceAll("[^0-9]", "");
                        int num = Integer.parseInt(numPart);
                        if (num > maxNum) maxNum = num;
                    } catch (NumberFormatException ignored) {
                        // IDs from the web app (e.g. "REQ-1234...") won't parse; that's fine
                    }
                }
            }

            // Tell Request to start new IDs above the highest loaded one
            Request.setCounter(maxNum + 1);

            System.out.printf("  📂 Loaded %d request(s) from %s%n",
                    requests.size(), dataFile.getFileName());

        } catch (IOException e) {
            System.err.println("  ⚠ Could not read requests file: " + e.getMessage());
        }
    }

    /**
     * Appends a single new request as one line at the end of the file.
     * Much faster than rewriting the whole file when just adding.
     *
     * @param request The request to append
     */
    private void appendToFile(Request request) {
        try {
            // Make sure the parent directory exists
            Files.createDirectories(dataFile.getParent());

            // Append the new line (CREATE file if it doesn't exist yet)
            Files.writeString(
                    dataFile,
                    request.toRecord() + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("  ⚠ Could not save request to file: " + e.getMessage());
        }
    }

    /**
     * Rewrites the entire requests file from scratch using the current in-memory list.
     * Called after every update or delete so the file stays in sync.
     */
    private void saveAllToFile() {
        try {
            // Build the full file content
            StringBuilder content = new StringBuilder();
            for (Request r : requests) {
                content.append(r.toRecord()).append(System.lineSeparator());
            }

            // Write (or overwrite) the file
            Files.writeString(
                    dataFile,
                    content.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            System.err.println("  ⚠ Could not save requests to file: " + e.getMessage());
        }
    }

    // ── Internal Utilities ───────────────────────────────────
    /**
     * Checks whether a given status string is one of the four allowed values.
     *
     * @param status The status string to validate
     * @return       true if the status is valid
     */
    private boolean isValidStatus(String status) {
        return status != null && (
                status.equalsIgnoreCase(Request.STATUS_PENDING)  ||
                status.equalsIgnoreCase(Request.STATUS_APPROVED) ||
                status.equalsIgnoreCase(Request.STATUS_REJECTED) ||
                status.equalsIgnoreCase(Request.STATUS_CANCELLED)
        );
    }

    /**
     * Returns the total number of requests currently in memory.
     *
     * @return Count of all requests
     */
    public int getTotalCount() {
        return requests.size();
    }

    /**
     * Returns the number of requests with Pending status.
     *
     * @return Count of pending requests
     */
    public int getPendingCount() {
        return findByStatus(Request.STATUS_PENDING).size();
    }
}
