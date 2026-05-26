
//  OOP CONCEPTS USED:
//    • Encapsulation  – menu logic is separated into helper methods
//    • Abstraction    – main() stays clean; details are hidden in helpers
//    • Polymorphism   – ReviewManager internally uses VerifiedReview/PublicReview
//
//  DEPENDENCIES:
//    • Review.java         (Review + VerifiedReview + PublicReview classes)
//    • ReviewManager.java  (CRUD + file handling logic)
//    • reviews.txt         (data file — auto-created if missing)

import java.util.Scanner;

public class AutoMartApplication {

    private static final Scanner scanner = new Scanner(System.in);

    // ReviewManager handles all business logic and file I/O 
    private static final ReviewManager manager = new ReviewManager();

    public static void main(String[] args) {
        printWelcomeBanner();

        boolean running = true;

        // Main program loop — keeps running until user chooses Exit
        while (running) {
            printMainMenu();

            int choice = readMenuChoice(0, 9);

            switch (choice) {
                case 1 -> handleSubmitFeedback();          // CREATE
                case 2 -> handleViewReviews();             // READ
                case 3 -> handleUpdateReview();            // UPDATE
                case 4 -> handleDeleteReview();            // DELETE
                case 5 -> handleRatingsMenu();             // RATINGS
                case 6 -> handleViewReviewById();          // READ by ID
                case 7 -> handleSearchByCar();             // READ by car
                case 8 -> manager.showSummaryStats();      // STATS
                case 9 -> handleReloadFromFile();          // RELOAD
                case 0 -> {
                    running = false;
                    printGoodbye();
                }
                default -> System.out.println("  [!] Invalid option. Please try again.");
            }
        }

        scanner.close();
    }

    // Prints the welcome banner shown once on startup
    private static void printWelcomeBanner() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════╗");
        System.out.println("  ║         🚗  AUTO MART APPLICATION  🚗           ║");
        System.out.println("  ║        Feedback & Review System v1.0             ║");
        System.out.println("  ╚══════════════════════════════════════════════════╝");
        System.out.println("  Loaded " + manager.getTotalReviews() + " existing review(s) from file.");
        System.out.println();
    }

    // Prints the main feedback menu
    private static void printMainMenu() {
        System.out.println();
        System.out.println("  ┌──────────────────────────────────────────────────┐");
        System.out.println("  │            FEEDBACK & REVIEW MENU                │");
        System.out.println("  ├──────────────────────────────────────────────────┤");
        System.out.println("  │  1. Submit Feedback (Add New Review)             │");
        System.out.println("  │  2. View All Reviews                             │");
        System.out.println("  │  3. Update a Review                              │");
        System.out.println("  │  4. Delete a Review                              │");
        System.out.println("  │  5. View Car & Seller Ratings                    │");
        System.out.println("  │  6. Find Review by ID                            │");
        System.out.println("  │  7. Search Reviews by Car Name                   │");
        System.out.println("  │  8. Show Summary Statistics                      │");
        System.out.println("  │  9. Reload Reviews from File                     │");
        System.out.println("  │  0. Exit                                         │");
        System.out.println("  └──────────────────────────────────────────────────┘");
        System.out.print("  Enter your choice: ");
    }

    // Prints the ratings sub-menu (cars vs sellers)
    private static void printRatingsMenu() {
        System.out.println();
        System.out.println("  ┌──────────────────────────────────────┐");
        System.out.println("  │         RATINGS SUB-MENU             │");
        System.out.println("  ├──────────────────────────────────────┤");
        System.out.println("  │  1. View Ratings by Car              │");
        System.out.println("  │  2. View Ratings by Seller/User      │");
        System.out.println("  │  3. Back to Main Menu                │");
        System.out.println("  └──────────────────────────────────────┘");
        System.out.print("  Enter your choice: ");
    }

    // Goodbye message shown when user exits
    private static void printGoodbye() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════╗");
        System.out.println("  ║  Thank you for using Auto Mart! 🚗   ║");
        System.out.println("  ║  All reviews have been saved.        ║");
        System.out.println("  ╚══════════════════════════════════════╝");
        System.out.println();
    }

    //  MENU HANDLER — Option 1: Submit Feedback (CREATE)
    private static void handleSubmitFeedback() {
        System.out.println("\n  ── SUBMIT NEW FEEDBACK ──────────────────────────");

        // Collect User ID
        System.out.print("  Enter your User ID (e.g. U001): ");
        String userId = scanner.nextLine().trim();

        // Collect car name
        System.out.print("  Enter Car Name (e.g. Toyota Prius): ");
        String carName = scanner.nextLine().trim();

        // Collect rating
        System.out.print("  Enter Rating (1 to 5 stars): ");
        int rating = readIntInRange(1, 5);

        // Collect comment
        System.out.print("  Enter your comment/feedback: ");
        String comment = scanner.nextLine().trim();

        // Ask for review type
        System.out.println();
        System.out.println("  Review Type:");
        System.out.println("    1. Verified Review  (you purchased/inspected the car)");
        System.out.println("    2. Public Review    (open community feedback)");
        System.out.print("  Enter choice (1 or 2): ");
        int typeChoice = readIntInRange(1, 2);
        boolean isVerified = (typeChoice == 1);

        // Delegate to ReviewManager
        manager.submitReview(userId, carName, rating, comment, isVerified);
    }

    //  MENU HANDLER — Option 2: View All Reviews (READ)
    private static void handleViewReviews() {
        manager.viewAllReviews();
    }
    //  MENU HANDLER — Option 3: Update a Review (UPDATE)
    private static void handleUpdateReview() {
        System.out.println("\n  ── UPDATE A REVIEW ──────────────────────────────");

        // Show existing reviews first so the user knows valid IDs
        manager.viewAllReviews();

        if (manager.getTotalReviews() == 0) {
            System.out.println("  Nothing to update yet.");
            return;
        }

        System.out.print("  Enter Review ID to update (e.g. RV001): ");
        String reviewId = scanner.nextLine().trim();

        // New comment (press Enter to skip)
        System.out.print("  New comment (press Enter to keep existing): ");
        String newComment = scanner.nextLine(); // keep raw — empty means no change

        // New rating (0 = keep existing)
        System.out.print("  New rating 1-5 (enter 0 to keep existing): ");
        int newRating = readIntInRange(0, 5);

        // Delegate to ReviewManager
        manager.updateReview(reviewId, newComment, newRating);
    }

    //  MENU HANDLER — Option 4: Delete a Review (DELETE)
    private static void handleDeleteReview() {
        System.out.println("\n  ── DELETE A REVIEW ──────────────────────────────");

        // Show reviews so the user can identify what to delete
        manager.viewAllReviews();

        if (manager.getTotalReviews() == 0) {
            System.out.println("  Nothing to delete yet.");
            return;
        }

        System.out.print("  Enter Review ID to delete (e.g. RV001): ");
        String reviewId = scanner.nextLine().trim();

        // Confirm before deleting
        System.out.print("  Are you sure you want to delete " + reviewId + "? (yes/no): ");
        String confirm = scanner.nextLine().trim();

        if (confirm.equalsIgnoreCase("yes") || confirm.equalsIgnoreCase("y")) {
            manager.deleteReview(reviewId);
        } else {
            System.out.println("  Deletion cancelled.");
        }
    }

    //  MENU HANDLER — Option 5: View Ratings (Cars & Sellers)
    private static void handleRatingsMenu() {
        boolean inRatingsMenu = true;

        while (inRatingsMenu) {
            printRatingsMenu();
            int choice = readMenuChoice(1, 3);

            switch (choice) {
                case 1 -> manager.showCarRatings();
                case 2 -> manager.showSellerRatings();
                case 3 -> inRatingsMenu = false;
                default -> System.out.println("  [!] Invalid option.");
            }
        }
    }

    //  MENU HANDLER — Option 6: Find Review by ID (READ)
    private static void handleViewReviewById() {
        System.out.println("\n  ── FIND REVIEW BY ID ────────────────────────────");
        System.out.print("  Enter Review ID (e.g. RV001): ");
        String reviewId = scanner.nextLine().trim();
        manager.viewReviewById(reviewId);
    }

    //  MENU HANDLER — Option 7: Search Reviews by Car Name (READ)
    private static void handleSearchByCar() {
        System.out.println("\n  ── SEARCH REVIEWS BY CAR NAME ───────────────────");
        System.out.print("  Enter car name or keyword (e.g. Prius): ");
        String carName = scanner.nextLine().trim();
        manager.viewReviewsByCar(carName);
    }

    //  MENU HANDLER — Option 9: Reload from File
    //  Useful if reviews.txt was edited externally
    private static void handleReloadFromFile() {
        System.out.println("\n  ── RELOAD FROM FILE ─────────────────────────────");
        manager.loadFromFile();
        System.out.println("  Reviews reloaded. Current count: " + manager.getTotalReviews());
    }

    //  INPUT HELPER — readMenuChoice()
    //  Safely reads an integer from the user within [min, max].
    //  Keeps prompting until valid input is entered.
    private static int readMenuChoice(int min, int max) {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                int choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) {
                    return choice;
                }
                System.out.printf("  [!] Please enter a number between %d and %d: ", min, max);
            } catch (NumberFormatException e) {
                System.out.printf("  [!] Invalid input. Enter a number (%d-%d): ", min, max);
            }
        }
    }

    //  INPUT HELPER — readIntInRange()
    //  Similar to readMenuChoice but prints no extra prompt.
    private static int readIntInRange(int min, int max) {
        while (true) {
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("  [!] Enter a value between %d and %d: ", min, max);
            } catch (NumberFormatException e) {
                System.out.printf("  [!] Not a number. Enter a value between %d and %d: ", min, max);
            }
        }
    }
}
