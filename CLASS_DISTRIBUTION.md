# AutoMart Class Distribution

This refactor separates the code that was previously embedded inside `AutoMartApplication.java` into dedicated source files.

## Component 01: User Management
- `AppUser.java`
- `BuyerUser.java`
- `SellerUser.java`
- `AdminUser.java`
- `UserManager.java`
- `SessionManager.java`

## Component 02: Car Listing Management
- `Vehicle.java`
- `FileHelper.java`
- `Status.java`

## Component 03: Linked List & Sorting
- `ISortable.java`
- `VehicleNode.java`
- `VehicleLinkedList.java`

## Component 04: Buying & Request Management
- `PurchaseRequest.java`

## Component 05: Admin Dashboard
- `AdminManager.java`
- `BaseManager.java`

## Component 06: Feedback & Review System
- `ReviewEntry.java`
- `VerifiedReview.java`
- `PublicReview.java`

## Application / Routing Layer
- `AutoMartApplication.java`
- `AppHandler.java`
- `Settings.java`
