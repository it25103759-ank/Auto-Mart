// ============================================================
//  AutoMart Application — Feedback & Review System
//  FILE: ReviewManager.java
//
//  PURPOSE:
//    This class manages ALL review operations:
//      • Create  – add a new review and save to file
//      • Read    – load reviews from file and display them
//      • Update  – edit comment/rating of an existing review
//      • Delete  – remove a review from file
//      • Ratings – show average ratings per car and per seller
//
//  OOP CONCEPTS USED:
//    • Encapsulation  – private list of reviews; accessed via public methods
//    • Abstraction    – caller doesn't need to know HOW file I/O works
//    • Inheritance    – uses Review, VerifiedReview, PublicReview
//    • Polymorphism   – calls getReviewTypeBadge() without knowing subclass type
//
//  FILE HANDLING:
//    • Reads from  reviews.txt  on startup (loadFromFile)
//    • Writes to   reviews.txt  after every change (saveToFile)
//    • Format: reviewId,userId,carName,rating,comment,reviewType,timestamp
// ============================================================

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReviewManager {

    // ── Constants ───────────────────────────────────────────────────────────────
    private static final String FILE_NAME      = "reviews.txt"; // file where reviews are stored
    private static final String SEPARATOR      = ",";           // delimiter used in the file
    private static final DateTimeFormatter FMT =               // timestamp format
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Internal list of reviews (Encapsulation) ─────────────────────────────
    // All reviews are kept in memory in this list.
    // The list is populated from file on start-up, and synced back on every change.
    private List<Review> reviews = new ArrayList<>();

    // ── Counter used to auto-generate unique review IDs ───────────────────────
    private int idCounter = 1;

    // ══════════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR — called once when AutoMartApplication starts up
    // ══════════════════════════════════════════════════════════════════════════
    public ReviewManager() {
        loadFromFile(); // load any existing reviews from reviews.txt
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FILE HANDLING — loadFromFile()
    //  Reads reviews.txt line by line and rebuilds Review objects in memory.
    //  Called automatically in the constructor.
    // ══════════════════════════════════════════════════════════════════════════
    public void loadFromFile() {
        reviews.clear(); // reset the list before loading

        File file = new File(FILE_NAME);

        // If the file doesn't exist yet, nothing to load — that's fine
        if (!file.exists()) {
            System.out.println("  [INFO] No reviews.txt found. Starting fresh.");
            return;
        }

        // BufferedReader reads the file line by line (efficient for large files)
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip blank lines or comment lines starting with #
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Parse the comma-separated values
                // Format: reviewId,userId,carName,rating,comment,reviewType,timestamp
                String[] parts = line.split(SEPARATOR, 7); // 7 fields max (comment may contain spaces)

                if (parts.length < 7) {
                    System.out.println("  [WARN] Skipping malformed line " + lineNumber + ": " + line);
                    continue;
                }

                // Extract each field
                String reviewId   = parts[0].trim();
                String userId     = parts[1].trim();
                String carName    = parts[2].trim();
                String reviewType = parts[5].trim();
                String timestamp  = parts[6].trim();

                // Parse rating safely (handle corrupt data)
                int rating = 5; // default if parsing fails
                try {
                    rating = Integer.parseInt(parts[3].trim());
                } catch (NumberFormatException e) {
                    System.out.println("  [WARN] Bad rating on line " + lineNumber + ", defaulting to 5.");
                }

                String comment = parts[4].trim();

                // Reconstruct the correct subclass based on reviewType
                Review review;
                if ("VerifiedReview".equalsIgnoreCase(reviewType)) {
                    review = new VerifiedReview(reviewId, userId, carName, rating, comment, timestamp);
                } else {
                    review = new PublicReview(reviewId, userId, carName, rating, comment, timestamp);
                }

                reviews.add(review);

                // Keep the ID counter ahead of existing IDs to avoid duplicates
                // Extract number from IDs like "RV001" → 1
                try {
                    int num = Integer.parseInt(reviewId.replaceAll("[^0-9]", ""));
                    if (num >= idCounter) idCounter = num + 1;
                } catch (NumberFormatException ignored) {}
            }

            System.out.println("  [INFO] Loaded " + reviews.size() + " review(s) from " + FILE_NAME);

        } catch (IOException e) {
            System.out.println("  [ERROR] Could not read " + FILE_NAME + ": " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FILE HANDLING — saveToFile()
    //  Writes all in-memory reviews back to reviews.txt.
    //  Called after every Create / Update / Delete operation.
    // ══════════════════════════════════════════════════════════════════════════
    public void saveToFile() {
        // PrintWriter overwrites the file entirely each time (simple approach)
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME, false))) {
            // Write a header comment so the file is human-readable
            writer.println("# AutoMart Reviews File");
            writer.println("# Format: reviewId,userId,carName,rating,comment,reviewType,timestamp");
            writer.println("# Example: RV001,U001,Toyota Prius,5,Excellent Condition,VerifiedReview,2025-05-24 10:30:00");
            writer.println();

            // Write each review as a comma-separated line
            for (Review review : reviews) {
                writer.println(review.toFileString());
            }

            // PrintWriter buffers output; flush ensures everything is written to disk
            writer.flush();

        } catch (IOException e) {
            System.out.println("  [ERROR] Could not save to " + FILE_NAME + ": " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPER — generateId()
    //  Creates a unique review ID like "RV001", "RV002", etc.
    // ══════════════════════════════════════════════════════════════════════════
    private String generateId() {
        // String.format pads the number with leading zeros to 3 digits
        String id = String.format("RV%03d", idCounter);
        idCounter++;
        return id;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPER — currentTimestamp()
    //  Returns the current date and time as a formatted string.
    // ══════════════════════════════════════════════════════════════════════════
    private String currentTimestamp() {
        return LocalDateTime.now().format(FMT);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPER — findById()
    //  Searches the list for a Review with a matching ID.
    //  Returns null if not found.
    // ══════════════════════════════════════════════════════════════════════════
    private Review findById(String reviewId) {
        for (Review review : reviews) {
            // Case-insensitive comparison
            if (review.getReviewId().equalsIgnoreCase(reviewId.trim())) {
                return review;
            }
        }
        return null; // not found
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CREATE — submitReview()
    //  Adds a brand-new review. Called from the main menu option 1.
    //
    //  Parameters:
    //    userId     – who is submitting (e.g. "U001")
    //    carName    – which car is being reviewed (e.g. "Toyota Prius")
    //    rating     – star rating 1–5
    //    comment    – written feedback
    //    isVerified – true = VerifiedReview, false = PublicReview
    // ══════════════════════════════════════════════════════════════════════════
    public void submitReview(String userId, String carName, int rating,
                             String comment, boolean isVerified) {

        // Validate input before creating the object
        if (userId == null || userId.trim().isEmpty()) {
            System.out.println("  [!] User ID cannot be empty.");
            return;
        }
        if (carName == null || carName.trim().isEmpty()) {
            System.out.println("  [!] Car name cannot be empty.");
            return;
        }
        if (rating < 1 || rating > 5) {
            System.out.println("  [!] Rating must be between 1 and 5.");
            return;
        }
        if (comment == null || comment.trim().isEmpty()) {
            System.out.println("  [!] Comment cannot be empty.");
            return;
        }

        // Remove commas from inputs to prevent file format corruption
        userId  = userId.trim().replace(",", "");
        carName = carName.trim().replace(",", "");
        comment = comment.trim().replace(",", "");

        // Generate a unique ID and get the current time
        String reviewId  = generateId();
        String timestamp = currentTimestamp();

        // Create either a VerifiedReview or PublicReview (Polymorphism)
        Review review;
        if (isVerified) {
            review = new VerifiedReview(reviewId, userId, carName, rating, comment, timestamp);
        } else {
            review = new PublicReview(reviewId, userId, carName, rating, comment, timestamp);
        }

        // Add to the in-memory list
        reviews.add(review);

        // Persist immediately to file
        saveToFile();

        System.out.println("\n  ✅ Review submitted successfully!");
        System.out.println("  Review ID: " + reviewId);
        review.displayReview();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  READ — viewAllReviews()
    //  Displays every review in the list.
    //  Called from main menu option 2.
    // ══════════════════════════════════════════════════════════════════════════
    public void viewAllReviews() {
        if (reviews.isEmpty()) {
            System.out.println("\n  No reviews found. Be the first to submit feedback!");
            return;
        }

        System.out.println("\n  ══════════ ALL REVIEWS (" + reviews.size() + " total) ══════════");
        for (Review review : reviews) {
            review.displayReview(); // calls subclass-specific displayReview (Polymorphism)
            System.out.println(); // blank line between reviews
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  READ — viewReviewById()
    //  Finds and displays a single review by its ID.
    // ══════════════════════════════════════════════════════════════════════════
    public void viewReviewById(String reviewId) {
        Review review = findById(reviewId);
        if (review == null) {
            System.out.println("  [!] Review ID '" + reviewId + "' not found.");
        } else {
            System.out.println("\n  ── Review Details ──");
            review.displayReview();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  READ — viewReviewsByCar()
    //  Displays all reviews for a specific car name.
    // ══════════════════════════════════════════════════════════════════════════
    public void viewReviewsByCar(String carName) {
        boolean found = false;
        System.out.println("\n  ── Reviews for: " + carName + " ──");

        for (Review review : reviews) {
            // Case-insensitive partial match (so "Prius" matches "Toyota Prius")
            if (review.getCarName().toLowerCase().contains(carName.toLowerCase())) {
                review.displayReview();
                System.out.println();
                found = true;
            }
        }

        if (!found) {
            System.out.println("  No reviews found for car: " + carName);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE — updateReview()
    //  Edits the comment and/or rating of an existing review.
    //  Called from main menu option 3.
    //
    //  Parameters:
    //    reviewId   – the ID of the review to update
    //    newComment – new feedback text (pass null to keep existing)
    //    newRating  – new star rating (pass 0 to keep existing)
    // ══════════════════════════════════════════════════════════════════════════
    public void updateReview(String reviewId, String newComment, int newRating) {
        Review review = findById(reviewId);

        if (review == null) {
            System.out.println("  [!] Review ID '" + reviewId + "' not found. Cannot update.");
            return;
        }

        boolean changed = false;

        // Update comment if a non-empty new comment was provided
        if (newComment != null && !newComment.trim().isEmpty()) {
            String sanitized = newComment.trim().replace(",", ""); // no commas allowed
            review.setComment(sanitized);
            System.out.println("  Comment updated.");
            changed = true;
        }

        // Update rating if a valid value (1–5) was provided
        if (newRating >= 1 && newRating <= 5) {
            review.setRating(newRating);
            System.out.println("  Rating updated to " + newRating + ".");
            changed = true;
        }

        if (!changed) {
            System.out.println("  [!] Nothing was updated (no valid changes provided).");
            return;
        }

        // Save the updated list back to file
        saveToFile();

        System.out.println("\n  ✅ Review updated successfully!");
        review.displayReview();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DELETE — deleteReview()
    //  Removes a review from the list and updates the file.
    //  Called from main menu option 4.
    // ══════════════════════════════════════════════════════════════════════════
    public void deleteReview(String reviewId) {
        Review toRemove = findById(reviewId);

        if (toRemove == null) {
            System.out.println("  [!] Review ID '" + reviewId + "' not found. Cannot delete.");
            return;
        }

        // Show what we're about to delete so the user can confirm
        System.out.println("\n  You are about to delete:");
        toRemove.displayReview();

        // Remove from in-memory list
        reviews.remove(toRemove);

        // Sync the file
        saveToFile();

        System.out.println("  🗑️  Review " + reviewId + " deleted successfully.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RATINGS — showCarRatings()
    //  Calculates and displays the average star rating for each car.
    //  This satisfies the "Add Ratings for Cars" requirement.
    //  Called from main menu option 5.
    // ══════════════════════════════════════════════════════════════════════════
    public void showCarRatings() {
        if (reviews.isEmpty()) {
            System.out.println("\n  No reviews yet. Submit some reviews first!");
            return;
        }

        System.out.println("\n  ══════════ CAR RATINGS SUMMARY ══════════");

        // Build a list of unique car names
        List<String> carNames = new ArrayList<>();
        for (Review review : reviews) {
            if (!carNames.contains(review.getCarName())) {
                carNames.add(review.getCarName());
            }
        }

        // For each car, compute the average rating
        for (String car : carNames) {
            int totalRating = 0;
            int count       = 0;

            for (Review review : reviews) {
                if (review.getCarName().equals(car)) {
                    totalRating += review.getRating();
                    count++;
                }
            }

            double average = (double) totalRating / count;

            // Build a star bar — full stars for the floor, check the decimal
            int fullStars = (int) average;
            StringBuilder stars = new StringBuilder();
            for (int i = 1; i <= 5; i++) {
                stars.append(i <= fullStars ? "★" : "☆");
            }

            System.out.printf("  %-35s | %s | Avg: %.1f/5 | Reviews: %d%n",
                    car, stars, average, count);
        }

        System.out.println("  ═════════════════════════════════════════");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RATINGS — showSellerRatings()
    //  Calculates and displays the average star rating for each seller/user.
    //  This satisfies the "Add Ratings for Sellers" requirement.
    //  Sellers are identified by their userId.
    //  Called from main menu option 5.
    // ══════════════════════════════════════════════════════════════════════════
    public void showSellerRatings() {
        if (reviews.isEmpty()) {
            System.out.println("\n  No reviews yet. Submit some reviews first!");
            return;
        }

        System.out.println("\n  ══════════ SELLER/USER RATINGS SUMMARY ══════════");

        // Collect unique user IDs
        List<String> userIds = new ArrayList<>();
        for (Review review : reviews) {
            if (!userIds.contains(review.getUserId())) {
                userIds.add(review.getUserId());
            }
        }

        // For each user, sum up all ratings they've given (as a reviewer)
        // In a real system this might be "ratings received as a seller";
        // here we group by the userId associated with each review.
        for (String uid : userIds) {
            int totalRating = 0;
            int count       = 0;
            List<String> carsReviewed = new ArrayList<>();

            for (Review review : reviews) {
                if (review.getUserId().equals(uid)) {
                    totalRating += review.getRating();
                    count++;
                    if (!carsReviewed.contains(review.getCarName())) {
                        carsReviewed.add(review.getCarName());
                    }
                }
            }

            double average = (double) totalRating / count;

            System.out.println("  ┌──────────────────────────────────────────────");
            System.out.printf ("  │  Seller/User : %s%n", uid);
            System.out.printf ("  │  Avg Rating  : %.1f/5  (%d reviews submitted)%n", average, count);
            System.out.println("  │  Cars rated  : " + String.join(", ", carsReviewed));
            System.out.println("  └──────────────────────────────────────────────");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UTILITY — getTotalReviews()
    //  Returns the number of reviews currently in memory.
    // ══════════════════════════════════════════════════════════════════════════
    public int getTotalReviews() {
        return reviews.size();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UTILITY — showSummaryStats()
    //  Shows a quick overview: total reviews, breakdown by type, overall avg.
    // ══════════════════════════════════════════════════════════════════════════
    public void showSummaryStats() {
        System.out.println("\n  ══════════ REVIEW SYSTEM STATS ══════════");
        System.out.println("  Total Reviews    : " + reviews.size());

        if (reviews.isEmpty()) {
            System.out.println("  ═════════════════════════════════════════");
            return;
        }

        int verified = 0, publicCount = 0, ratingSum = 0;
        for (Review review : reviews) {
            if (review instanceof VerifiedReview) verified++;
            else publicCount++;
            ratingSum += review.getRating();
        }

        double overallAvg = (double) ratingSum / reviews.size();

        System.out.println("  Verified Reviews : " + verified);
        System.out.println("  Public Reviews   : " + publicCount);
        System.out.printf ("  Overall Avg ★    : %.1f / 5%n", overallAvg);
        System.out.println("  ═════════════════════════════════════════");
    }
}
