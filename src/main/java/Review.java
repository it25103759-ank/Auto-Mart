// ============================================================
//  AutoMart Application — Feedback & Review System
//  FILE: Review.java
//
//  PURPOSE:
//    This file defines the Review class, which is the core
//    data model (blueprint) for a single review entry.
//
//  OOP CONCEPTS USED:
//    • Class & Objects        – Review is a blueprint; each review is an object
//    • Encapsulation          – fields are private; accessed via getters/setters
//    • Inheritance            – VerifiedReview and PublicReview extend Review
//    • Polymorphism           – getReviewTypeBadge() is overridden in subclasses
//    • Abstraction            – getReviewTypeBadge() forces subclasses to define
//                               their own badge label
//
//  FILE FORMAT (reviews.txt):
//    reviewId,userId,carName,rating,comment,reviewType,timestamp
//    Example: RV001,U001,Toyota Prius,5,Excellent Condition,VerifiedReview,2025-05-24T10:30:00
// ============================================================

public abstract class Review {

    // ── Private fields (Encapsulation)

    private String reviewId;       // Unique review ID
    private String userId;         // User who wrote the review
    private String carName;        // Name of the car being reviewed
    private int    rating;         // Star rating from 1 to 5
    private String comment;        // The written feedback text
    private String reviewType;     // "VerifiedReview" or "PublicReview"
    private String timestamp;      // Date/time string when review was created

    // ── Constructor
    // Called when a new Review object is created.
    // Subclasses call super(...) to fill in all fields.
    public Review(String reviewId, String userId, String carName,
                  int rating, String comment, String reviewType, String timestamp) {
        this.reviewId   = reviewId;
        this.userId     = userId;
        this.carName    = carName;
        this.rating     = rating;
        this.comment    = comment;
        this.reviewType = reviewType;
        this.timestamp  = timestamp;
    }

    // ── Abstract method (Abstraction + Polymorphism)
    // Every subclass MUST override this method and return its own badge label.
    // This is Polymorphism: same method name, different behaviour per subclass.
    public abstract String getReviewTypeBadge();

    // ── Getters (Encapsulation – read access to private fields)

    public String getReviewId()   { return reviewId;   }
    public String getUserId()     { return userId;     }
    public String getCarName()    { return carName;    }
    public int    getRating()     { return rating;     }
    public String getComment()    { return comment;    }
    public String getReviewType() { return reviewType; }
    public String getTimestamp()  { return timestamp;  }

    // ── Setters (Encapsulation – controlled write access)
    // Only comment and rating can be updated (for the Update feature).

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setRating(int rating) {
        // Validate: rating must be between 1 and 5
        if (rating >= 1 && rating <= 5) {
            this.rating = rating;
        } else {
            System.out.println("  [!] Rating must be between 1 and 5. Keeping old value.");
        }
    }

    // ── toFileString()
    // Converts the Review object into a comma-separated line for saving
    // to the reviews.txt file.
    // Format: reviewId,userId,carName,rating,comment,reviewType,timestamp
    public String toFileString() {
        return reviewId + "," + userId + "," + carName + "," +
               rating  + "," + comment + "," + reviewType + "," + timestamp;
    }

    // ── generateStars()
    // Returns a visual star string like "★★★★☆" based on the rating.
    public String generateStars() {
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            stars.append(i <= rating ? "★" : "☆");
        }
        return stars.toString();
    }

    // ── displayReview()
    // Prints a nicely formatted summary of this review to the console.
    public void displayReview() {
        System.out.println("  ┌─────────────────────────────────────────────────");
        System.out.println("  │  Review ID   : " + reviewId);
        System.out.println("  │  User ID     : " + userId);
        System.out.println("  │  Car         : " + carName);
        System.out.println("  │  Rating      : " + generateStars() + " (" + rating + "/5)");
        System.out.println("  │  Comment     : " + comment);
        System.out.println("  │  Type        : " + getReviewTypeBadge());   // polymorphism in action
        System.out.println("  │  Date        : " + timestamp);
        System.out.println("  └─────────────────────────────────────────────────");
    }

    // ── toString()
    @Override
    public String toString() {
        return "[" + reviewId + "] " + carName + " | " +
               generateStars() + " | " + userId + " | " + getReviewTypeBadge();
    }
}


// ╔══════════════════════════════════════════════════════════════╗
//  VerifiedReview — subclass of Review (Inheritance)
//  Represents a review written by a buyer who actually purchased
//  or formally inspected the vehicle.
// ╚══════════════════════════════════════════════════════════════╝
class VerifiedReview extends Review {

    // Constructor — calls the parent constructor via super()
    public VerifiedReview(String reviewId, String userId, String carName,
                          int rating, String comment, String timestamp) {
        super(reviewId, userId, carName, rating, comment, "VerifiedReview", timestamp);
    }

    // Polymorphism: this subclass returns its own badge label
    @Override
    public String getReviewTypeBadge() {
        return "✅ Verified Purchase Review";
    }
}


// ╔══════════════════════════════════════════════════════════════╗
//  PublicReview — subclass of Review (Inheritance)
//  Represents an open community review that anyone can submit.
// ╚══════════════════════════════════════════════════════════════╝
class PublicReview extends Review {

    // Constructor — calls the parent constructor via super()
    public PublicReview(String reviewId, String userId, String carName,
                        int rating, String comment, String timestamp) {
        super(reviewId, userId, carName, rating, comment, "PublicReview", timestamp);
    }

    // Polymorphism: this subclass returns its own badge label
    @Override
    public String getReviewTypeBadge() {
        return "🌐 Public Community Review";
    }
}
