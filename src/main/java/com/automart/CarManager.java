package com.automart;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CarManager.java
 * Handles all CRUD operations for Car listings and persists them in cars.txt.
 *
 * Responsibilities:
 *   - Load all cars from the file into memory (List<Car>)
 *   - Save the in-memory list back to the file
 *   - Create / Read / Update / Delete individual cars
 *   - Search by brand, model, or max price
 */
public class CarManager {

    // Path to the plain-text data file
    private static final String FILE_NAME = "cars.txt";

    // In-memory list — all operations work on this list, then flush to file
    private List<Car> carList;

    // ── Constructor ──────────────────────────────────────────────────────────
    public CarManager() {
        carList = new ArrayList<>();
        loadFromFile();   // read existing data into memory on startup
    }

    // =========================================================================
    //  FILE HANDLING
    // =========================================================================

    /** Read every line from cars.txt and rebuild the Car objects. */
    private void loadFromFile() {
        File file = new File(FILE_NAME);

        // Create an empty file if it doesn't exist yet
        if (!file.exists()) {
            try {
                file.createNewFile();
                System.out.println("[Info] cars.txt created fresh.");
            } catch (IOException e) {
                System.out.println("[Error] Could not create cars.txt: " + e.getMessage());
            }
            return;
        }

        // File exists — read line by line
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;           // skip blank lines
                Car car = Car.fromFileString(line);
                if (car != null) carList.add(car);
            }
        } catch (IOException e) {
            System.out.println("[Error] Reading cars.txt: " + e.getMessage());
        }
    }

    /** Overwrite cars.txt with the current in-memory list. */
    private void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, false))) {
            for (Car car : carList) {
                writer.write(car.toFileString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("[Error] Writing cars.txt: " + e.getMessage());
        }
    }

    // =========================================================================
    //  CREATE
    // =========================================================================

    /**
     * Add a brand-new car listing.
     * Returns false if the ID already exists (no duplicates allowed).
     */
    public boolean addCar(Car car) {
        // Guard: ID must be unique
        if (findById(car.getCarId()) != null) {
            System.out.println("[Error] Car ID '" + car.getCarId() + "' already exists.");
            return false;
        }
        carList.add(car);
        saveToFile();
        System.out.println("[Success] Car added: " + car.getCarId());
        return true;
    }

    // =========================================================================
    //  READ  (display helpers)
    // =========================================================================

    /** Print every car in a formatted table. */
    public void displayAllCars() {
        if (carList.isEmpty()) {
            System.out.println("  No car listings found.");
            return;
        }
        printTableHeader();
        for (Car car : carList) {
            System.out.println(car);
        }
        printTableFooter();
        System.out.println("  Total: " + carList.size() + " listing(s).");
    }

    /** Find one car by its ID; returns null if not found. */
    public Car findById(String carId) {
        for (Car car : carList) {
            if (car.getCarId().equalsIgnoreCase(carId)) return car;
        }
        return null;
    }

    // =========================================================================
    //  SEARCH
    // =========================================================================

    /** Search by brand (case-insensitive, partial match). */
    public List<Car> searchByBrand(String brand) {
        List<Car> results = new ArrayList<>();
        for (Car car : carList) {
            if (car.getBrand().toLowerCase().contains(brand.toLowerCase())) {
                results.add(car);
            }
        }
        return results;
    }

    /** Search by model (case-insensitive, partial match). */
    public List<Car> searchByModel(String model) {
        List<Car> results = new ArrayList<>();
        for (Car car : carList) {
            if (car.getModel().toLowerCase().contains(model.toLowerCase())) {
                results.add(car);
            }
        }
        return results;
    }

    /**
     * Search by maximum price — returns all cars at or below that price,
     * sorted cheapest first.
     */
    public List<Car> searchByMaxPrice(double maxPrice) {
        List<Car> results = new ArrayList<>();
        for (Car car : carList) {
            if (car.getPrice() <= maxPrice) {
                results.add(car);
            }
        }
        // Simple insertion sort (ascending price) — beginner-friendly
        for (int i = 1; i < results.size(); i++) {
            Car key = results.get(i);
            int j = i - 1;
            while (j >= 0 && results.get(j).getPrice() > key.getPrice()) {
                results.set(j + 1, results.get(j));
                j--;
            }
            results.set(j + 1, key);
        }
        return results;
    }

    /** Print a list of cars as a table; shows "No results" if empty. */
    public void displaySearchResults(List<Car> results, String label) {
        System.out.println("\n  Search results for: " + label);
        if (results.isEmpty()) {
            System.out.println("  No matching cars found.");
            return;
        }
        printTableHeader();
        for (Car car : results) System.out.println(car);
        printTableFooter();
        System.out.println("  Found: " + results.size() + " listing(s).");
    }

    // =========================================================================
    //  UPDATE
    // =========================================================================

    /**
     * Update mutable fields of an existing car.
     * Pass null / 0 for any field you do NOT want to change.
     * Returns false if the car ID is not found.
     */
    public boolean updateCar(String carId, String brand, String model,
                             int year, double price, String ownerName) {
        Car car = findById(carId);
        if (car == null) {
            System.out.println("[Error] Car ID '" + carId + "' not found.");
            return false;
        }

        // Only overwrite non-null / non-zero values
        if (brand     != null && !brand.isBlank())     car.setBrand(brand);
        if (model     != null && !model.isBlank())     car.setModel(model);
        if (year      > 0)                             car.setYear(year);
        if (price     > 0)                             car.setPrice(price);
        if (ownerName != null && !ownerName.isBlank()) car.setOwnerName(ownerName);

        saveToFile();
        System.out.println("[Success] Car '" + carId + "' updated.");
        return true;
    }

    //  DELETE

    public boolean deleteCar(String carId) {
        Car car = findById(carId);
        if (car == null) {
            System.out.println("[Error] Car ID '" + carId + "' not found.");
            return false;
        }
        carList.remove(car);
        saveToFile();
        System.out.println("[Success] Car '" + carId + "' deleted.");
        return true;
    }


    private void printTableHeader() {
        System.out.println();
        System.out.println("+---------+--------------+--------------+------+-----------------+-----------------+");
        System.out.println("| Car ID  | Brand        | Model        | Year | Price (LKR)     | Owner           |");
        System.out.println("+---------+--------------+--------------+------+-----------------+-----------------+");
    }

    private void printTableFooter() {
        System.out.println("+---------+--------------+--------------+------+-----------------+-----------------+");
    }
}
