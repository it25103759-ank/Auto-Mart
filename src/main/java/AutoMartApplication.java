// ============================================================
//  AutoMartApplication.java
//  AutoMart Application — Buying & Request Management
//  Main entry point: displays menu, reads input, calls manager.
// ============================================================

package com.automart;

import java.util.List;
import java.util.Scanner;

/**
 * AutoMartApplication is the entry point of the console-based
 * Buying & Request Management system.
 *
 * It:
 *   1. Creates a RequestManager (which loads the file automatically)
 *   2. Shows a menu loop until the user chooses to exit
 *   3. Delegates every real action to RequestManager
 *
 * OOP concepts demonstrated:
 *   - Objects     : RequestManager object does the heavy lifting
 *   - Scanner     : reads keyboard input from the user
 *   - Loops       : while loop keeps the menu running
 *   - Conditionals: switch/if statements handle menu choices
 */
public class AutoMartApplication {

    // ── Pre-loaded sample vehicles ───────────────────────────
    // In a full app these would come from the vehicle database.
    // Here we hard-code a few so the demo works without the web server.
    private static final String[][] SAMPLE_VEHICLES = {
            { "suv-1",        "Land Rover Defender 110"       },
            { "suv-2",        "Toyota Land Cruiser Prado TX"  },
            { "sedan-1",      "BMW 320i"                      },
            { "sedan-2",      "Mercedes-Benz CLA 180"         },
            { "crossover-1",  "Jeep Renegade Limited"         },
            { "hybrid-3",     "Toyota Prius S Touring"        },
            { "pickup-2",     "Ford F-150 Raptor Night Edition"},
            { "hatchback-2",  "Classic Mini Cooper"           },
    };

    // ── Shared Scanner (one Scanner for the whole program) ───
    private static final Scanner scanner = new Scanner(System.in);

    // ── Main ─────────────────────────────────────────────────
    public static void main(String[] args) {

        printBanner();

        // Create the manager — it automatically loads requests.txt
        RequestManager manager = new RequestManager();

        System.out.println();

        // Menu loop — keeps running until the user types 0
        boolean running = true;
        while (running) {
            printMenu(manager);
            String choice = readLine("Enter your choice: ");

            switch (choice) {
                case "1" -> handleSendRequest(manager);
                case "2" -> handleViewAll(manager);
                case "3" -> handleViewStatus(manager);
                case "4" -> handleApprove(manager);
                case "5" -> handleReject(manager);
                case "6" -> handleCancel(manager);
                case "7" -> handleDelete(manager);
                case "8" -> handleViewByBuyer(manager);
                case "0" -> {
                    System.out.println("\n  👋 Goodbye! Thank you for using AutoMart.");
                    running = false;
                }
                default  -> System.out.println("\n  ⚠  Invalid choice. Please enter a number from the menu.");
            }

            if (running) pause(); // small pause so the user can read the output
        }

        scanner.close();
    }

    // ════════════════════════════════════════════════════════
    //  MENU ACTIONS
    // ════════════════════════════════════════════════════════

    // ── 1. Send Purchase Request (CREATE) ────────────────────
    /**
     * Asks the user to pick a vehicle and enter a note,
     * then creates a new request via the manager.
     */
    private static void handleSendRequest(RequestManager manager) {
        printSectionHeader("SEND PURCHASE REQUEST");

        // Show the vehicle list so the user can pick one
        System.out.println("  Available Vehicles:");
        System.out.println("  ─────────────────────────────────────────────");
        for (int i = 0; i < SAMPLE_VEHICLES.length; i++) {
            System.out.printf("  [%d] %-12s — %s%n",
                    i + 1,
                    SAMPLE_VEHICLES[i][0],
                    SAMPLE_VEHICLES[i][1]);
        }
        System.out.println("  ─────────────────────────────────────────────");

        // Read vehicle choice
        String indexStr = readLine("  Select vehicle number (1-" + SAMPLE_VEHICLES.length + "): ");
        int index;
        try {
            index = Integer.parseInt(indexStr.trim()) - 1;
            if (index < 0 || index >= SAMPLE_VEHICLES.length) {
                System.out.println("  ❌ Invalid selection.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("  ❌ Please enter a valid number.");
            return;
        }

        String vehicleId    = SAMPLE_VEHICLES[index][0];
        String vehicleTitle = SAMPLE_VEHICLES[index][1];

        // Read buyer username
        String buyer = readLine("  Your username (buyer): ");
        if (buyer.isBlank()) {
            System.out.println("  ❌ Username cannot be empty.");
            return;
        }

        // Read optional note
        String note = readLine("  Note / message (optional, press Enter to skip): ");
        if (note.isBlank()) note = "No note provided.";

        // Delegate to RequestManager
        manager.createRequest(vehicleId, vehicleTitle, buyer, note);
    }

    // ── 2. View All Requests (READ) ───────────────────────────
    /**
     * Prints every request in the system to the console.
     */
    private static void handleViewAll(RequestManager manager) {
        printSectionHeader("ALL PURCHASE REQUESTS");
        manager.printAllRequests();
    }

    // ── 3. View Request Status (READ by ID) ──────────────────
    /**
     * Asks the user for a request ID and displays that single request.
     */
    private static void handleViewStatus(RequestManager manager) {
        printSectionHeader("VIEW REQUEST STATUS");

        String id = readLine("  Enter Request ID (e.g. R001): ");
        Request found = manager.findById(id);

        if (found == null) {
            System.out.println("  ❌ No request found with ID: " + id);
        } else {
            System.out.println();
            System.out.println(found);
        }
    }

    // ── 4. Approve Request (UPDATE) ──────────────────────────
    /**
     * Approves a request by its ID.
     */
    private static void handleApprove(RequestManager manager) {
        printSectionHeader("APPROVE REQUEST");

        String id = readLine("  Enter Request ID to approve: ");
        manager.approveRequest(id.trim());
    }

    // ── 5. Reject Request (UPDATE) ───────────────────────────
    /**
     * Rejects a request by its ID.
     */
    private static void handleReject(RequestManager manager) {
        printSectionHeader("REJECT REQUEST");

        String id = readLine("  Enter Request ID to reject: ");
        manager.rejectRequest(id.trim());
    }

    // ── 6. Cancel Request (UPDATE) ───────────────────────────
    /**
     * Cancels a request by its ID (buyer-initiated).
     */
    private static void handleCancel(RequestManager manager) {
        printSectionHeader("CANCEL REQUEST");

        String id = readLine("  Enter Request ID to cancel: ");
        manager.cancelRequest(id.trim());
    }

    // ── 7. Delete Request (DELETE) ───────────────────────────
    /**
     * Permanently deletes a request after confirmation.
     */
    private static void handleDelete(RequestManager manager) {
        printSectionHeader("DELETE REQUEST");

        String id = readLine("  Enter Request ID to delete: ");

        // Show the request first so the user knows what they're deleting
        Request found = manager.findById(id);
        if (found == null) {
            System.out.println("  ❌ No request found with ID: " + id);
            return;
        }

        System.out.println("\n  You are about to permanently delete:");
        System.out.println(found);
        String confirm = readLine("\n  Type YES to confirm deletion: ");

        if ("YES".equalsIgnoreCase(confirm.trim())) {
            manager.deleteRequest(id.trim());
        } else {
            System.out.println("  ↩  Deletion cancelled.");
        }
    }

    // ── 8. View Requests by Buyer ────────────────────────────
    /**
     * Shows all requests made by a specific buyer username.
     */
    private static void handleViewByBuyer(RequestManager manager) {
        printSectionHeader("VIEW REQUESTS BY BUYER");

        String buyer = readLine("  Enter buyer username: ");
        List<Request> results = manager.findByBuyer(buyer);

        if (results.isEmpty()) {
            System.out.println("  (No requests found for buyer: " + buyer + ")");
        } else {
            System.out.printf("%n  ── %d request(s) for '%s' ──%n%n",
                    results.size(), buyer);
            for (Request r : results) {
                System.out.println(r);
                System.out.println();
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  UI HELPERS
    // ════════════════════════════════════════════════════════

    /**
     * Prints the main ASCII banner at startup.
     */
    private static void printBanner() {
        System.out.println("""
                ╔══════════════════════════════════════════════════════╗
                ║          🚗  AUTO MART — REQUEST MANAGEMENT          ║
                ║         Buying & Purchase Request Console App         ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }

    /**
     * Prints the numbered menu with a live count of total / pending requests.
     *
     * @param manager Used to show live counts next to the menu header
     */
    private static void printMenu(RequestManager manager) {
        System.out.printf("""
                ┌──────────────────────────────────────────────────────┐
                │  MAIN MENU          Total: %-5d  Pending: %-5d      │
                ├──────────────────────────────────────────────────────┤
                │  1. 📨  Send Purchase Request     (Create)           │
                │  2. 📋  View All Requests          (Read — all)      │
                │  3. 🔍  View Request by ID         (Read — one)      │
                │  4. ✅  Approve a Request          (Update)          │
                │  5. ❌  Reject a Request           (Update)          │
                │  6. 🚫  Cancel a Request           (Update)          │
                │  7. 🗑️  Delete a Request           (Delete)          │
                │  8. 👤  View Requests by Buyer     (Read — filter)   │
                │  0. 🚪  Exit                                         │
                └──────────────────────────────────────────────────────┘
                """,
                manager.getTotalCount(),
                manager.getPendingCount());
    }

    /**
     * Prints a section header to visually separate menu actions.
     *
     * @param title The section title
     */
    private static void printSectionHeader(String title) {
        System.out.println("\n  ══════════════════════════════════════════");
        System.out.println("   " + title);
        System.out.println("  ══════════════════════════════════════════");
    }

    /**
     * Prints a prompt and reads a line of text from the user.
     * Never returns null — returns an empty string if nothing is typed.
     *
     * @param prompt The prompt to display
     * @return       The trimmed user input
     */
    private static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine() : "";
    }

    /**
     * Waits for the user to press Enter before showing the menu again.
     * Keeps the console readable.
     */
    private static void pause() {
        System.out.println("\n  Press Enter to return to the menu...");
        if (scanner.hasNextLine()) scanner.nextLine();
    }
}
