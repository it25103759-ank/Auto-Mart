// ============================================================
//  Request.java
//  AutoMart Application — Buying & Request Management
//  Model class representing a single purchase request.
// ============================================================

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Request is a plain data class (a "model") that holds
 * all the information about one purchase request.
 *
 * OOP concepts used:
 *   - Encapsulation  : fields are private; accessed via getters/setters
 *   - Constructor    : builds a Request object from individual pieces
 *   - static factory : create() builds a brand-new request with auto-ID
 */
public class Request {

    // ── Constants ────────────────────────────────────────────
    /** Formatter used when storing / reading dates in the text file. */
    public static final DateTimeFormatter FILE_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** Formatter used when displaying dates to the user. */
    public static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    /** The tab character that separates fields in requests.txt */
    public static final String SEPARATOR = "\t";

    // Allowed status values
    public static final String STATUS_PENDING  = "Pending";
    public static final String STATUS_APPROVED = "Approved";
    public static final String STATUS_REJECTED = "Rejected";
    public static final String STATUS_CANCELLED = "Cancelled";

    // ── Private fields (Encapsulation) ───────────────────────
    private String id;           // Unique request ID, e.g. "R001"
    private String vehicleId;    // ID of the vehicle being requested
    private String vehicleTitle; // Human-readable vehicle name
    private String buyerUsername;// Username of the buyer
    private String note;         // Optional message from the buyer
    private String status;       // Pending / Approved / Rejected / Cancelled
    private String createdAt;    // Timestamp when the request was made

    // ── Counter for auto-generating IDs ──────────────────────
    /** Shared counter; starts at 1 and increases for every new request. */
    private static int counter = 1;

    // ── Constructor ──────────────────────────────────────────
    /**
     * Full constructor — used when loading an existing request from the file.
     *
     * @param id            Unique request ID (e.g. "R001")
     * @param vehicleId     Vehicle identifier
     * @param vehicleTitle  Vehicle display name
     * @param buyerUsername Buyer's username
     * @param note          Buyer's note / message
     * @param status        Current status of the request
     * @param createdAt     ISO date-time string
     */
    public Request(String id, String vehicleId, String vehicleTitle,
                   String buyerUsername, String note,
                   String status, String createdAt) {
        this.id            = id;
        this.vehicleId     = vehicleId;
        this.vehicleTitle  = vehicleTitle;
        this.buyerUsername = buyerUsername;
        this.note          = note;
        this.status        = status;
        this.createdAt     = createdAt;
    }

    // ── Static Factory Method ────────────────────────────────
    /**
     * Creates a brand-new Request with an auto-generated ID and the
     * current timestamp. Status is always "Pending" for a new request.
     *
     * @param vehicleId     Vehicle identifier
     * @param vehicleTitle  Vehicle display name
     * @param buyerUsername Buyer's username
     * @param note          Buyer's note
     * @return              A new Request object ready to be saved
     */
    public static Request create(String vehicleId, String vehicleTitle,
                                  String buyerUsername, String note) {
        // Format the ID as R001, R002, R003 …
        String newId = String.format("R%03d", counter++);
        String timestamp = LocalDateTime.now().format(FILE_FORMAT);
        return new Request(newId, vehicleId, vehicleTitle,
                           buyerUsername, note, STATUS_PENDING, timestamp);
    }

    // ── File Serialization ───────────────────────────────────
    /**
     * Converts this Request into a single tab-separated line for storage.
     * Example: R001\tC001\tLand Rover Defender\tbuyer1\tInterested\tPending\t2026-01-01T10:00
     *
     * @return A tab-separated string representing this request
     */
    public String toRecord() {
        return String.join(SEPARATOR,
                id,
                vehicleId,
                clean(vehicleTitle),
                clean(buyerUsername),
                clean(note),
                status,
                createdAt);
    }

    /**
     * Parses a tab-separated line from requests.txt back into a Request object.
     * Returns null if the line is invalid (too few fields).
     *
     * @param line A single line from requests.txt
     * @return     A Request object, or null if parsing fails
     */
    public static Request fromRecord(String line) {
        if (line == null || line.isBlank()) return null;

        // Split on tab; -1 keeps empty trailing fields
        String[] parts = line.split(SEPARATOR, -1);

        // We need at least 7 fields to build a valid Request
        if (parts.length < 7) return null;

        return new Request(
                parts[0],   // id
                parts[1],   // vehicleId
                parts[2],   // vehicleTitle
                parts[3],   // buyerUsername
                parts[4],   // note
                parts[5],   // status
                parts[6]    // createdAt
        );
    }

    // ── Display Helper ───────────────────────────────────────
    /**
     * Returns a nicely formatted multi-line summary of this request,
     * suitable for printing in the console.
     *
     * @return Formatted string
     */
    @Override
    public String toString() {
        // Try to parse and reformat the date for nicer display
        String displayDate;
        try {
            displayDate = LocalDateTime
                    .parse(createdAt, FILE_FORMAT)
                    .format(DISPLAY_FORMAT);
        } catch (Exception e) {
            displayDate = createdAt; // fall back to raw string
        }

        return String.format(
                "┌─────────────────────────────────────────────┐%n" +
                "  ID       : %-30s%n" +
                "  Vehicle  : %s (ID: %s)%n" +
                "  Buyer    : %-30s%n" +
                "  Note     : %-30s%n" +
                "  Status   : %-30s%n" +
                "  Created  : %-30s%n" +
                "└─────────────────────────────────────────────┘",
                id,
                vehicleTitle, vehicleId,
                buyerUsername,
                note,
                status,
                displayDate
        );
    }

    // ── Getters ──────────────────────────────────────────────
    public String getId()            { return id; }
    public String getVehicleId()     { return vehicleId; }
    public String getVehicleTitle()  { return vehicleTitle; }
    public String getBuyerUsername() { return buyerUsername; }
    public String getNote()          { return note; }
    public String getStatus()        { return status; }
    public String getCreatedAt()     { return createdAt; }

    // ── Setters ──────────────────────────────────────────────
    /** Updates the status (Pending / Approved / Rejected / Cancelled). */
    public void setStatus(String status) { this.status = status; }

    /** Updates the buyer's note. */
    public void setNote(String note) { this.note = note; }

    // ── Internal Utility ─────────────────────────────────────
    /**
     * Removes tab characters from a string so it doesn't break
     * the tab-separated file format.
     *
     * @param value The string to clean
     * @return      The cleaned string
     */
    private static String clean(String value) {
        if (value == null) return "";
        return value.replace("\t", " ").replace("\n", " ").replace("\r", "").trim();
    }

    /**
     * Sets the global counter to a value higher than any existing ID,
     * so that new IDs don't collide with loaded ones.
     * Called by RequestManager after it loads existing requests.
     *
     * @param nextValue The next safe counter value
     */
    public static void setCounter(int nextValue) {
        if (nextValue > counter) {
            counter = nextValue;
        }
    }
}
