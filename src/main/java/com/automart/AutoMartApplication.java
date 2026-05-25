import java.util.Scanner;

public class AutoMartApplication {

    // Shared Scanner – one instance used throughout the program
    private static final Scanner scanner = new Scanner(System.in);
    private static final CarManager carManager = new CarManager();

    // ════════════════════════════════════════════════════════════════════════
    //  MAIN
    // ════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║      🚗  Welcome to AutoMart System  🚗      ║");
        System.out.println("║       Car Listing Management Platform        ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        boolean running = true;

        while (running) {
            printMainMenu();
            int choice = readInt("  Enter your choice: ");

            switch (choice) {
                case 1:
                    handleAddCar();
                    break;
                case 2:
                    handleSearchMenu();
                    break;
                case 3:
                    carManager.viewAllCars();
                    break;
                case 4:
                    handleUpdateCar();
                    break;
                case 5:
                    handleDeleteCar();
                    break;
                case 6:
                    System.out.println("\n  Thank you for using AutoMart! Goodbye 👋");
                    running = false;
                    break;
                default:
                    System.out.println("⚠  Invalid choice. Please enter a number from 1 to 6.");
            }
        }

        scanner.close();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MENU DISPLAY
    // ════════════════════════════════════════════════════════════════════════

    private static void printMainMenu() {
        System.out.println("\n╔══════════════════════════════════╗");
        System.out.println("║         MAIN MENU                ║");
        System.out.println("╠══════════════════════════════════╣");
        System.out.println("║  1. Add New Car Listing          ║");
        System.out.println("║  2. Search Cars                  ║");
        System.out.println("║  3. View All Cars                ║");
        System.out.println("║  4. Update Car Details           ║");
        System.out.println("║  5. Delete Car Listing           ║");
        System.out.println("║  6. Exit                         ║");
        System.out.println("╚══════════════════════════════════╝");
    }

    private static void printSearchMenu() {
        System.out.println("\n  ── Search Options ──");
        System.out.println("  1. Search by Brand");
        System.out.println("  2. Search by Model");
        System.out.println("  3. Search by Price Range");
        System.out.println("  4. Back to Main Menu");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HANDLER  –  1. ADD CAR
    // ════════════════════════════════════════════════════════════════════════

    private static void handleAddCar() {
        System.out.println("\n══════════  ADD NEW CAR LISTING  ══════════");

        System.out.print("  Car ID      (e.g. C001) : ");
        String carId = scanner.nextLine().trim();

        System.out.print("  Brand       (e.g. Toyota): ");
        String brand = scanner.nextLine().trim();

        System.out.print("  Model       (e.g. Prius) : ");
        String model = scanner.nextLine().trim();

        int year = readInt("  Year        (e.g. 2020) : ");

        double price = readDouble("  Price (Rs.) (e.g. 6500000): ");

        System.out.print("  Owner Name  (e.g. Kasun) : ");
        String ownerName = scanner.nextLine().trim();

        // Basic validation
        if (carId.isEmpty() || brand.isEmpty() || model.isEmpty() || ownerName.isEmpty()) {
            System.out.println("❌  All fields are required. Please try again.");
            return;
        }
        if (year < 1886 || year > 2100) {          // 1886 = first car ever made
            System.out.println("❌  Please enter a valid year.");
            return;
        }
        if (price <= 0) {
            System.out.println("❌  Price must be greater than zero.");
            return;
        }

        carManager.addCar(carId, brand, model, year, price, ownerName);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HANDLER  –  2. SEARCH CARS
    // ════════════════════════════════════════════════════════════════════════

    private static void handleSearchMenu() {
        printSearchMenu();
        int choice = readInt("  Enter search option: ");

        switch (choice) {
            case 1:
                System.out.print("  Enter Brand to search: ");
                String brand = scanner.nextLine().trim();
                carManager.searchByBrand(brand);
                break;

            case 2:
                System.out.print("  Enter Model keyword to search: ");
                String model = scanner.nextLine().trim();
                carManager.searchByModel(model);
                break;

            case 3:
                double minPrice = readDouble("  Enter Minimum Price (Rs.): ");
                double maxPrice = readDouble("  Enter Maximum Price (Rs.): ");
                if (minPrice > maxPrice) {
                    System.out.println("❌  Minimum price cannot be greater than maximum price.");
                } else {
                    carManager.searchByPrice(minPrice, maxPrice);
                }
                break;

            case 4:
                // Go back
                break;

            default:
                System.out.println("⚠  Invalid search option.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HANDLER  –  4. UPDATE CAR
    // ════════════════════════════════════════════════════════════════════════

    private static void handleUpdateCar() {
        System.out.println("\n══════════  UPDATE CAR DETAILS  ══════════");
        System.out.print("  Enter Car ID to update: ");
        String carId = scanner.nextLine().trim();

        // Show existing details first
        var existing = carManager.findCarById(carId);
        if (existing == null) {
            System.out.println("❌  Car ID '" + carId + "' not found.");
            return;
        }

        System.out.println("\n  Current details:");
        existing.displayCar();
        System.out.println("  (Press ENTER to keep the current value for any field)\n");

        // Collect new values – blank input means "keep existing"
        System.out.print("  New Brand       [" + existing.getBrand() + "]: ");
        String newBrand = scanner.nextLine().trim();

        System.out.print("  New Model       [" + existing.getModel() + "]: ");
        String newModel = scanner.nextLine().trim();

        System.out.print("  New Year        [" + existing.getYear() + "]: ");
        String yearInput = scanner.nextLine().trim();
        int newYear = yearInput.isEmpty() ? 0 : Integer.parseInt(yearInput);

        System.out.print("  New Price (Rs.) [" + existing.getPrice() + "]: ");
        String priceInput = scanner.nextLine().trim();
        double newPrice = priceInput.isEmpty() ? 0 : Double.parseDouble(priceInput);

        System.out.print("  New Owner Name  [" + existing.getOwnerName() + "]: ");
        String newOwnerName = scanner.nextLine().trim();

        carManager.updateCar(carId, newBrand, newModel, newYear, newPrice, newOwnerName);

        // Show the updated record
        System.out.println("\n  Updated details:");
        carManager.findCarById(carId).displayCar();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HANDLER  –  5. DELETE CAR
    // ════════════════════════════════════════════════════════════════════════

    private static void handleDeleteCar() {
        System.out.println("\n══════════  DELETE CAR LISTING  ══════════");
        System.out.print("  Enter Car ID to delete: ");
        String carId = scanner.nextLine().trim();

        // Show the record before deleting
        var car = carManager.findCarById(carId);
        if (car == null) {
            System.out.println("❌  Car ID '" + carId + "' not found.");
            return;
        }

        System.out.println("\n  Car to be deleted:");
        car.displayCar();

        // Confirm before deleting
        System.out.print("  Are you sure you want to delete this listing? (yes/no): ");
        String confirm = scanner.nextLine().trim();

        if (confirm.equalsIgnoreCase("yes") || confirm.equalsIgnoreCase("y")) {
            carManager.deleteCar(carId);
        } else {
            System.out.println("  Deletion cancelled.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILITY HELPERS  –  Safe input reading
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Reads an integer from the user. Keeps asking until a valid int is entered.
     */
    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("⚠  Please enter a valid whole number.");
            }
        }
    }


    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("⚠  Please enter a valid number.");
            }
        }
    }
}
