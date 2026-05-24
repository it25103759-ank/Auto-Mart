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

abstract class AppUser {
    private String username;
    private String email;
    private String phone;
    private String passwordHash;
    private final String role;
    private String status = "active";
    private String banReason = "";
    private String adminNote = "";

    AppUser(String username, String email, String phone, String password, String role) {
        this.username = safe(username).trim();
        this.email = safe(email).trim();
        this.phone = safe(phone).trim();
        this.passwordHash = hashPassword(password);
        this.role = role;
    }

    String getUsername() { return username; }
    String getEmail() { return email; }
    String getPhone() { return phone; }
    String getPassword() { return passwordHash; }
    String getRole() { return role; }
    String getStatus() { return status == null || status.isBlank() ? "active" : status; }
    String getBanReason() { return banReason == null ? "" : banReason; }
    String getAdminNote() { return adminNote == null ? "" : adminNote; }
    void setUsername(String username) { this.username = safe(username).trim(); }
    void setEmail(String email) { this.email = safe(email).trim(); }
    void setPhone(String phone) { this.phone = safe(phone).trim(); }
    void setStatus(String status) { this.status = safe(status).trim().isBlank() ? "active" : safe(status).trim().toLowerCase(Locale.ROOT); }
    void setBanReason(String reason) { this.banReason = safe(reason).trim(); }
    void setAdminNote(String note) { this.adminNote = safe(note).trim(); }
    boolean isBanned() { return "banned".equalsIgnoreCase(getStatus()); }
    void setPassword(String password) { this.passwordHash = hashPassword(password); }
    boolean checkPassword(String candidate) { return hashPassword(candidate).equals(passwordHash); }
    boolean isAdmin() { return "admin".equalsIgnoreCase(role); }
    String toRecord() { return String.join("	", role, Vehicle.clean(username), Vehicle.clean(email), Vehicle.clean(phone), Vehicle.clean(passwordHash), Vehicle.clean(getStatus()), Vehicle.clean(getBanReason()), Vehicle.clean(getAdminNote())); }
    static AppUser fromRecord(String line) {
        String[] p = line.split("\t", -1);
        if (p.length < 5) return null;
        AppUser user = switch (p[0].toLowerCase(Locale.ROOT)) {
            case "admin" -> new AdminUser(p[1], p[2], p[3], p[4]);
            case "seller" -> new SellerUser(p[1], p[2], p[3], p[4]);
            default -> new BuyerUser(p[1], p[2], p[3], p[4]);
        };
        if (p.length > 5 && !p[5].isBlank()) user.setStatus(p[5]);
        if (p.length > 6) user.setBanReason(p[6]);
        if (p.length > 7) user.setAdminNote(p[7]);
        return user;
    }
    abstract String roleMessage();
}
