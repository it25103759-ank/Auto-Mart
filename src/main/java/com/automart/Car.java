package com.automart;


public class Car {

    //  Fields (private for encapsulation)
    private String carId;       // Unique identifier, e.g. C001
    private String brand;       // e.g. Toyota
    private String model;       // e.g. Prius
    private int    year;        // e.g. 2020
    private double price;       // Price in LKR
    private String ownerName;   // e.g. Kasun

    // Constructor (used when creating a new Car object)
    public Car(String carId, String brand, String model, int year, double price, String ownerName) {
        this.carId     = carId;
        this.brand     = brand;
        this.model     = model;
        this.year      = year;
        this.price     = price;
        this.ownerName = ownerName;
    }

    // Getters (read the private fields from outside)
    public String getCarId()     { return carId; }
    public String getBrand()     { return brand; }
    public String getModel()     { return model; }
    public int    getYear()      { return year; }
    public double getPrice()     { return price; }
    public String getOwnerName() { return ownerName; }

    // Setters (update the private fields from outside)
    public void setBrand(String brand)         { this.brand     = brand; }
    public void setModel(String model)         { this.model     = model; }
    public void setYear(int year)              { this.year      = year; }
    public void setPrice(double price)         { this.price     = price; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    // ── Convert Car → one CSV line for storage
    // Format:  C001,Toyota,Prius,2020,6500000.0,Kasun
    public String toFileString() {
        return carId + "," + brand + "," + model + "," + year + "," + price + "," + ownerName;
    }

    // Rebuild a Car from one CSV line read from the file
    public static Car fromFileString(String line) {
        // Split on commas; expect exactly 6 parts
        String[] parts = line.split(",", -1);
        if (parts.length < 6) return null;           // skip malformed lines

        String carId     = parts[0].trim();
        String brand     = parts[1].trim();
        String model     = parts[2].trim();
        int    year      = Integer.parseInt(parts[3].trim());
        double price     = Double.parseDouble(parts[4].trim());
        String ownerName = parts[5].trim();

        return new Car(carId, brand, model, year, price, ownerName);
    }

    // Pretty-print for console display
    @Override
    public String toString() {
        return String.format(
            "| %-6s | %-12s | %-12s | %4d | %,14.2f LKR | %-15s |",
            carId, brand, model, year, price, ownerName
        );
    }
}
