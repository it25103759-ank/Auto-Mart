public class Car {

    // ── Fields ──────────────────────────────────────────────────────────────
    private String carId;       // Unique ID  e.g. C001
    private String brand;       // e.g. Toyota
    private String model;       // e.g. Prius
    private int    year;        // e.g. 2020
    private double price;       // e.g. 6500000
    private String ownerName;   // e.g. Kasun

    // ── Constructor (all fields) ─────────────────────────────────────────────
    public Car(String carId, String brand, String model,
               int year, double price, String ownerName) {
        this.carId     = carId;
        this.brand     = brand;
        this.model     = model;
        this.year      = year;
        this.price     = price;
        this.ownerName = ownerName;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public String getCarId()     { return carId;     }
    public String getBrand()     { return brand;     }
    public String getModel()     { return model;     }
    public int    getYear()      { return year;      }
    public double getPrice()     { return price;     }
    public String getOwnerName() { return ownerName; }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setBrand(String brand)         { this.brand     = brand;     }
    public void setModel(String model)         { this.model     = model;     }
    public void setYear(int year)              { this.year      = year;      }
    public void setPrice(double price)         { this.price     = price;     }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }


    public String toFileString() {
        return carId + "," + brand + "," + model + "," +
               year  + "," + price + "," + ownerName;
    }


    public static Car fromFileString(String line) {
        String[] parts = line.split(",");
        String carId     = parts[0].trim();
        String brand     = parts[1].trim();
        String model     = parts[2].trim();
        int    year      = Integer.parseInt(parts[3].trim());
        double price     = Double.parseDouble(parts[4].trim());
        String ownerName = parts[5].trim();
        return new Car(carId, brand, model, year, price, ownerName);
    }


    public void displayCar() {
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.printf ("│  Car ID   : %-27s │%n", carId);
        System.out.printf ("│  Brand    : %-27s │%n", brand);
        System.out.printf ("│  Model    : %-27s │%n", model);
        System.out.printf ("│  Year     : %-27d │%n", year);
        System.out.printf ("│  Price    : Rs. %-23.2f │%n", price);
        System.out.printf ("│  Owner    : %-27s │%n", ownerName);
        System.out.println("└─────────────────────────────────────────┘");
    }
}
