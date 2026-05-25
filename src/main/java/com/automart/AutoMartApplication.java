package com.automart;

import java.util.List;
import java.util.Scanner;


public class AutoMartApplication {

    // Shared Scanner — one instance for the whole application
    private static final Scanner scanner = new Scanner(System.in);

    // CarManager handles all CRUD logic and file I/O
    private static final CarManager carManager = new CarManager();

    // ── Program entry point ──────────────────────────────────────────────────
    public static void main(String[] args) {

        printBanner();

        boolean running = true;

        while (running) {
            printMenu();

            int choice = readInt("  Your choice: ");

            switch (choice) {
                case 1  -> addCarMenu();
                case 2  -> viewAllCars();
                case 3  -> searchByBrandMenu();
                case 4  -> searchByModelMenu();
                case 5  -> searchByPriceMenu();
                case 6  -> updateCarMenu();
                case 7  -> deleteCarMenu();
                case 0  -> { System.out.println("\n  Goodbye! Thank you for using AutoMart.\n"); running = false; }
                default -> System.out.println("  [!] Invalid choice. Please enter 0-7.");
            }
        }

        scanner.close();
    }

    // =========================================================================
    //  MENU SCREENS
    // =========================================================================


    private static void printBanner() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║     🚗  AutoMart — Car Listing System    ║");
        System.out.println("  ║     Colombo, Sri Lanka                   ║");
        System.out.println("  ╚══════════════════════════════════════════╝");
        System.out.println();
    }


    private static void printMenu() {
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │           MAIN MENU                     │");
        System.out.println("  ├─────────────────────────────────────────┤");
        System.out.println("  │  1. Add New Car Listing                 │");
        System.out.println("  │  2. View All Cars                       │");
        System.out.println("  │  3. Search by Brand                     │");
        System.out.println("  │  4. Search by Model                     │");
        System.out.println("  │  5. Search by Max Price                 │");
        System.out.println("  │  6. Update Car Details                  │");
        System.out.println("  │  7. Delete Car Listing                  │");
        System.out.println("  │  0. Exit                                │");
        System.out.println("  └─────────────────────────────────────────┘");
    }

    // =========================================================================
    //  1. ADD CAR
    // =========================================================================
    private static void addCarMenu() {
        System.out.println("\n  ── Add New Car Listing ──────────────────────");

        String carId     = readString("  Car ID       (e.g. C001)      : ");
        String brand     = readString("  Brand        (e.g. Toyota)    : ");
        String model     = readString("  Model        (e.g. Prius)     : ");
        int    year      = readInt   ("  Year         (e.g. 2020)      : ");
        double price     = readDouble("  Price in LKR (e.g. 6500000)   : ");
        String ownerName = readString("  Owner Name   (e.g. Kasun)     : ");

        // Basic validation
        if (carId.isBlank() || brand.isBlank() || model.isBlank()) {
            System.out.println("  [!] Car ID, Brand, and Model cannot be empty.");
            return;
        }
        if (year < 1900 || year > 2100) {
            System.out.println("  [!] Please enter a valid year.");
            return;
        }
        if (price <= 0) {
            System.out.println("  [!] Price must be greater than zero.");
            return;
        }

        Car newCar = new Car(carId, brand, model, year, price, ownerName);
        carManager.addCar(newCar);
    }

    // =========================================================================
    //  2. VIEW ALL CARS
    // =========================================================================
    private static void viewAllCars() {
        System.out.println("\n  ── All Car Listings ─────────────────────────");
        carManager.displayAllCars();
    }

    // =========================================================================
    //  3. SEARCH BY BRAND
    // =========================================================================
    private static void searchByBrandMenu() {
        System.out.println("\n  ── Search by Brand ──────────────────────────");
        String brand = readString("  Enter brand to search: ");
        List<Car> results = carManager.searchByBrand(brand);
        carManager.displaySearchResults(results, "Brand = \"" + brand + "\"");
    }

    // =========================================================================
    //  4. SEARCH BY MODEL
    // =========================================================================
    private static void searchByModelMenu() {
        System.out.println("\n  ── Search by Model ──────────────────────────");
        String model = readString("  Enter model to search: ");
        List<Car> results = carManager.searchByModel(model);
        carManager.displaySearchResults(results, "Model = \"" + model + "\"");
    }

    // =========================================================================
    //  5. SEARCH BY MAX PRICE
    // =========================================================================
    private static void searchByPriceMenu() {
        System.out.println("\n  ── Search by Maximum Price ──────────────────");
        double maxPrice = readDouble("  Enter maximum price (LKR): ");
        if (maxPrice <= 0) {
            System.out.println("  [!] Price must be greater than zero.");
            return;
        }
        List<Car> results = carManager.searchByMaxPrice(maxPrice);
        carManager.displaySearchResults(results,
            "Price ≤ LKR " + String.format("%,.2f", maxPrice));
    }

    // =========================================================================
    //  6. UPDATE CAR
    // =========================================================================
    private static void updateCarMenu() {
        System.out.println("\n  ── Update Car Details ───────────────────────");

        // First show the list so the user knows which IDs exist
        carManager.displayAllCars();

        String carId = readString("  Enter Car ID to update: ");
        Car existing = carManager.findById(carId);
        if (existing == null) {
            System.out.println("  [!] Car ID '" + carId + "' not found.");
            return;
        }

        System.out.println("  (Leave blank to keep the current value)");
        System.out.printf ("  Current brand      : %s%n", existing.getBrand());
        String brand     = readString("  New brand          : ");

        System.out.printf ("  Current model      : %s%n", existing.getModel());
        String model     = readString("  New model          : ");

        System.out.printf ("  Current year       : %d%n", existing.getYear());
        String yearStr   = readString("  New year (0 = skip): ");
        int year         = yearStr.isBlank() ? 0 : parseIntSafe(yearStr);

        System.out.printf ("  Current price (LKR): %.2f%n", existing.getPrice());
        String priceStr  = readString("  New price (0 = skip): ");
        double price     = priceStr.isBlank() ? 0 : parseDoubleSafe(priceStr);

        System.out.printf ("  Current owner      : %s%n", existing.getOwnerName());
        String ownerName = readString("  New owner name     : ");

        carManager.updateCar(carId, brand, model, year, price, ownerName);
    }

    // =========================================================================
    //  7. DELETE CAR
    // =========================================================================
    private static void deleteCarMenu() {
        System.out.println("\n  ── Delete Car Listing ───────────────────────");
        carManager.displayAllCars();

        String carId = readString("  Enter Car ID to delete: ");

        // Confirmation prompt
        String confirm = readString("  Are you sure you want to delete '" + carId + "'? (yes/no): ");
        if (!"yes".equalsIgnoreCase(confirm.trim())) {
            System.out.println("  Deletion cancelled.");
            return;
        }

        carManager.deleteCar(carId);
    }
    


    private static String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }


    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Please enter a whole number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Please enter a valid number.");
            }
        }
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private static double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }
}
