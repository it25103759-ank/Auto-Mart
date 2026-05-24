package com.automart;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;


import static com.automart.AutoMartApplication.*;

final class Vehicle {

    final String id;
    final String category;
    final String title;
    final String price;
    final String year;
    final String mileage;
    final String fuel;
    final String transmission;
    final String description;
    final String image;
    final String createdAt;
    final boolean uploaded;
    final Status status;

    Vehicle(String id, String category, String title, String price, String year, String mileage, String fuel, String transmission, String description, String image, String createdAt, boolean uploaded) {
        this(id, category, title, price, year, mileage, fuel, transmission, description, image, createdAt, uploaded, Status.AVAILABLE);
    }

    Vehicle(String id, String category, String title, String price, String year, String mileage, String fuel, String transmission, String description, String image, String createdAt, boolean uploaded, Status status) {
        this.id=id; this.category=category; this.title=title; this.price=price; this.year=year; this.mileage=mileage; this.fuel=fuel; this.transmission=transmission; this.description=description; this.image=image; this.createdAt=createdAt; this.uploaded=uploaded; this.status=status==null?Status.AVAILABLE:status;
    }

    String toTsv() {
        return String.join("	", clean(id), clean(category), clean(title), clean(price), clean(year), clean(mileage), clean(fuel), clean(transmission), clean(description), clean(image), clean(createdAt), String.valueOf(uploaded), status.name());
    }

    static Vehicle fromTsv(String line) {
        String[] p = line.split("\t", -1);
        if (p.length < 12) return null;
        Status status = p.length >= 13 ? Status.fromValue(p[12]) : Status.AVAILABLE;
        return new Vehicle(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], Boolean.parseBoolean(p[11]), status);
    }

    static String clean(String s) {
        return s == null ? "" : s.replace("\t", " ").replace("\n", " ");
    }

    String toText() {
        return "Title: " + title + "\n"
                + "Category: " + category + "\n"
                + "Price: " + formatLkr(price) + "\n"
                + "Year: " + year + "\n"
                + "Mileage: " + mileage + "\n"
                + "Fuel: " + fuel + "\n"
                + "Transmission: " + transmission + "\n"
                + "Status: " + status.label + "\n"
                + "Created At: " + createdAt + "\n"
                + "Description: " + description + "\n"
                + "Image: " + image + "\n";
    }
}
