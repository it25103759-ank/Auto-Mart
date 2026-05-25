import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CarManager {

    // ── Constants ────────────────────────────────────────────────────────────
    private static final String FILE_NAME = "cars.txt";

    // ── In-memory list – loaded once from file ───────────────────────────────
    private List<Car> carList;

    // ── Constructor ──────────────────────────────────────────────────────────
    public CarManager() {
        carList = new ArrayList<>();
        loadCarsFromFile(); // Load existing records on startup
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FILE HANDLING  –  Load & Save
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Reads all car records from cars.txt into the in-memory list.
     * Called once when CarManager is created.
     */
    private void loadCarsFromFile() {
        File file = new File(FILE_NAME);

        // If the file doesn't exist yet, nothing to load
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip blank lines
                if (!line.isEmpty()) {
                    Car car = Car.fromFileString(line);
                    carList.add(car);
                }
            }
        } catch (IOException e) {
            System.out.println("⚠  Error loading cars from file: " + e.getMessage());
        }
    }

    /**
     * Writes the entire in-memory list back to cars.txt.
     * Called after every Create / Update / Delete operation.
     */
    private void saveCarsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Car car : carList) {
                writer.write(car.toFileString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("⚠  Error saving cars to file: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CREATE  –  Add New Car
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Adds a brand-new car listing.
     * Returns false if the Car ID already exists.
     */
    public boolean addCar(String carId, String brand, String model,
                          int year, double price, String ownerName) {

        // Check for duplicate Car ID
        if (findCarById(carId) != null) {
            System.out.println("❌  Car ID '" + carId + "' already exists. Use a unique ID.");
            return false;
        }

        Car newCar = new Car(carId, brand, model, year, price, ownerName);
        carList.add(newCar);
        saveCarsToFile();
        System.out.println("✅  Car listing added successfully!");
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  READ  –  View / Search Cars
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Displays every car currently stored.
     */
    public void viewAllCars() {
        if (carList.isEmpty()) {
            System.out.println("ℹ  No car listings found.");
            return;
        }
        System.out.println("\n══════════  ALL CAR LISTINGS (" + carList.size() + ")  ══════════");
        for (Car car : carList) {
            car.displayCar();
        }
    }

    /**
     * Searches for cars whose brand matches the given keyword (case-insensitive).
     */
    public void searchByBrand(String brand) {
        List<Car> results = new ArrayList<>();
        for (Car car : carList) {
            if (car.getBrand().equalsIgnoreCase(brand)) {
                results.add(car);
            }
        }
        printSearchResults(results, "Brand: " + brand);
    }


    public void searchByModel(String model) {
        List<Car> results = new ArrayList<>();
        for (Car car : carList) {
            if (car.getModel().toLowerCase().contains(model.toLowerCase())) {
                results.add(car);
            }
        }
        printSearchResults(results, "Model: " + model);
    }


    public void searchByPrice(double minPrice, double maxPrice) {
        List<Car> results = new ArrayList<>();
        for (Car car : carList) {
            if (car.getPrice() >= minPrice && car.getPrice() <= maxPrice) {
                results.add(car);
            }
        }
        printSearchResults(results,
                "Price Range: Rs." + minPrice + " – Rs." + maxPrice);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UPDATE  –  Edit Car Details
    // ════════════════════════════════════════════════════════════════════════


    public boolean updateCar(String carId, String newBrand, String newModel,
                             int newYear, double newPrice, String newOwnerName) {

        Car car = findCarById(carId);
        if (car == null) {
            System.out.println("❌  Car ID '" + carId + "' not found.");
            return false;
        }

        // Only update fields that were actually provided
        if (newBrand     != null && !newBrand.isEmpty())     car.setBrand(newBrand);
        if (newModel     != null && !newModel.isEmpty())     car.setModel(newModel);
        if (newYear      > 0)                                car.setYear(newYear);
        if (newPrice     > 0)                                car.setPrice(newPrice);
        if (newOwnerName != null && !newOwnerName.isEmpty()) car.setOwnerName(newOwnerName);

        saveCarsToFile();
        System.out.println("✅  Car listing updated successfully!");
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DELETE  –  Remove Car Listing
    // ════════════════════════════════════════════════════════════════════════


    public boolean deleteCar(String carId) {
        Car car = findCarById(carId);
        if (car == null) {
            System.out.println("❌  Car ID '" + carId + "' not found.");
            return false;
        }
        carList.remove(car);
        saveCarsToFile();
        System.out.println("✅  Car listing deleted successfully!");
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ════════════════════════════════════════════════════════════════════════


    public Car findCarById(String carId) {
        for (Car car : carList) {
            if (car.getCarId().equalsIgnoreCase(carId)) {
                return car;
            }
        }
        return null;
    }


    private void printSearchResults(List<Car> results, String criteria) {
        System.out.println("\n══════════  SEARCH RESULTS – " + criteria + "  ══════════");
        if (results.isEmpty()) {
            System.out.println("ℹ  No cars found matching your search.");
        } else {
            System.out.println("  Found " + results.size() + " result(s):\n");
            for (Car car : results) {
                car.displayCar();
            }
        }
    }
}
