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

class AppHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        try {
            if (path.startsWith("/assets/")) {
                serveStatic(exchange, STATIC_DIR.resolve(path.substring("/assets/".length())));
                return;
            }
            if ("/image-proxy".equals(path)) {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
                serveRemoteImage(exchange, params.getOrDefault("url", ""));
                return;
            }
            if ("/auth".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                html(exchange, authPage(null, false));
                return;
            }
            if ("/admin/login".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                html(exchange, adminLoginPage(null));
                return;
            }
            if ("/admin/signup".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                html(exchange, adminSignupPage(null, Map.of()));
                return;
            }
            if ("/admin/signup".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                String adminCode = safe(form.getOrDefault("adminCode", "")).trim();
                String fullName = safe(form.getOrDefault("fullName", "")).trim();
                String username = safe(form.getOrDefault("username", "")).trim();
                String email = normalizeEmailAddress(form.getOrDefault("email", ""));
                String phone = safe(form.getOrDefault("phone", "")).trim();
                String password = safe(form.getOrDefault("password", "")).trim();
                String confirmPassword = safe(form.getOrDefault("confirmPassword", "")).trim();
                if (adminCode.isBlank() || fullName.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
                    html(exchange, adminSignupPage("Fill all admin signup fields before continuing.", form));
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    html(exchange, adminSignupPage("Password and confirm password must match.", form));
                    return;
                }
                String error = registerAdminUser(username, email, phone, password, adminCode);
                if (error != null) {
                    html(exchange, adminSignupPage(error, form));
                    return;
                }
                startSession(exchange, username);
                redirect(exchange, "/?msg=" + url("Admin account created successfully."));
                return;
            }
            if ("/admin/login".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                String username = safe(form.getOrDefault("username", "")).trim();
                String password = safe(form.getOrDefault("password", "")).trim();
                if (username.isBlank() || password.isBlank()) {
                    html(exchange, adminLoginPage("Enter admin username and password.", ""));
                    return;
                }
                AppUser user = authenticateUser(username, password);
                if (user == null || !user.isAdmin()) {
                    html(exchange, adminLoginPage("Authorized staff only. Use an admin account.", ""));
                    return;
                }
                startSession(exchange, user.getUsername());
                redirect(exchange, "/");
                return;
            }
            if ("/auth".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                String username = safe(form.getOrDefault("username", "")).trim();
                String password = safe(form.getOrDefault("password", "")).trim();
                if (username.isBlank() || password.isBlank()) {
                    html(exchange, authPage("Enter your username/email and password to continue.", false, form));
                    return;
                }
                AppUser user = authenticateUser(username, password);
                if (user == null) {
                    html(exchange, authPage("Invalid username/email or password.", false, form));
                    return;
                }
                startSession(exchange, user.getUsername());
                redirect(exchange, "/");
                return;
            }
            if ("/signup".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                html(exchange, authPage(null, true));
                return;
            }
            if ("/signup".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                String username = safe(form.getOrDefault("username", "")).trim();
                String email = normalizeEmailAddress(form.getOrDefault("email", ""));
                String role = safe(form.getOrDefault("role", "buyer")).trim().toLowerCase(Locale.ROOT);
                if (!"seller".equals(role)) role = "buyer";
                String password = safe(form.getOrDefault("password", "")).trim();
                String confirmPassword = safe(form.getOrDefault("confirmPassword", "")).trim();
                String phone = safe(form.getOrDefault("phone", "")).trim();
                String fullName = safe(form.getOrDefault("fullName", "")).trim();
                String businessName = safe(form.getOrDefault("businessName", "")).trim();
                String shopLocation = safe(form.getOrDefault("shopLocation", "")).trim();
                String sellerNic = safe(form.getOrDefault("sellerNic", "")).trim();
                if (username.isBlank() || password.isBlank() || email.isBlank()) {
                    html(exchange, authPage("Create a username, email, and password to enter Auto Mart.", true, form));
                    return;
                }
                if ("buyer".equals(role) && fullName.isBlank()) {
                    html(exchange, authPage("Enter your full name for the buyer account.", true, form));
                    return;
                }
                if ("seller".equals(role) && (businessName.isBlank() || shopLocation.isBlank() || sellerNic.isBlank())) {
                    html(exchange, authPage("Enter seller name, shop location, and NIC / ID number.", true, form));
                    return;
                }
                if ("seller".equals(role) && !isValidSriLankanNic(sellerNic)) {
                    html(exchange, authPage("Enter a valid Sri Lankan NIC. Use 12 digits or 9 digits followed by V/X.", true, form));
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    html(exchange, authPage("Password and confirm password must match.", true, form));
                    return;
                }
                String signUpError = registerUser(username, email, phone, password, role);
                if (signUpError != null) {
                    html(exchange, authPage(signUpError, true, form));
                    return;
                }
                startSession(exchange, username);
                redirect(exchange, "/?signupSuccess=" + url(role));
                return;
            }
            if ("/social-auth".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
                String provider = safe(params.getOrDefault("provider", "google")).trim().toLowerCase(Locale.ROOT);
                String mode = safe(params.getOrDefault("mode", "signin")).trim().toLowerCase(Locale.ROOT);
                String role = safe(params.getOrDefault("role", "buyer")).trim().toLowerCase(Locale.ROOT);
                if (!"seller".equals(role) && !"buyer".equals(role)) {
                    role = "buyer";
                }
                if ("gmail".equals(provider)) {
                    provider = "google";
                }
                String username = provider + "_demo_" + role;
                if (findUser(username) == null) {
                    String socialError = registerUser(username, provider + ".demo." + role + "@gmail.com", "0770000000", "DemoPass1A", role);
                    if (socialError != null) {
                        html(exchange, authPage("Gmail demo sign-in failed: " + socialError, "signup".equals(mode), Map.of("role", role)));
                        return;
                    }
                }
                startSession(exchange, username);
                redirect(exchange, "/?social=" + url(provider) + "&mode=" + url(mode) + "&role=" + url(role));
                return;
            }
            if ("/logout".equals(path)) {
                clearSession(exchange);
                redirect(exchange, "/auth");
                return;
            }
            if (!isAuthenticated(exchange)) {
                if (path.startsWith("/admin")) {
                    redirect(exchange, "/admin/login");
                } else {
                    redirect(exchange, "/auth");
                }
                return;
            }
            if ("/".equals(path)) {
                html(exchange, page(exchange, "Home", homeContent(exchange), true));
                return;
            }
            if ("/inventory".equals(path)) {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
                html(exchange, page(exchange, "Inventory", inventoryContent(params), true));
                return;
            }
            if ("/wishlist".equals(path)) {
                html(exchange, page(exchange, "Wishlist", wishlistContent(), true));
                return;
            }
            if ("/sell".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                AppUser current = currentUser(exchange);
                if (current != null && !current.isAdmin() && !"seller".equalsIgnoreCase(current.getRole())) {
                    redirect(exchange, "/buyer-profile?error=" + url("Buyer accounts cannot access Sell Car. Use a seller account to list vehicles."));
                    return;
                }
                html(exchange, page(exchange, "Sell Your Car", sellContent(null), true));
                return;
            }
            if ("/sell".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                AppUser current = currentUser(exchange);
                if (current != null && !current.isAdmin() && !"seller".equalsIgnoreCase(current.getRole())) {
                    redirect(exchange, "/buyer-profile?error=" + url("Buyer accounts cannot submit vehicle listings."));
                    return;
                }
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                Vehicle vehicle = saveSubmission(form);
                redirect(exchange, "/vehicle?id=" + url(vehicle.id));
                return;
            }
            if ("/vehicle".equals(path)) {
                String id = parseQuery(exchange.getRequestURI().getRawQuery()).get("id");
                Vehicle v = findById(id);
                if (v == null) {
                    notFound(exchange);
                    return;
                }
                html(exchange, page(exchange, v.title, detailContent(v), true));
                return;
            }
            if ("/vehicle/edit".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String id = parseQuery(exchange.getRequestURI().getRawQuery()).get("id");
                Vehicle v = findById(id);
                if (v == null) {
                    notFound(exchange);
                    return;
                }
                if (!v.uploaded) {
                    html(exchange, page(exchange, "Vehicle Locked", lockedVehicleContent(v), true));
                    return;
                }
                html(exchange, page(exchange, "Edit Vehicle", editVehicleContent(v, null), true));
                return;
            }
            if ("/vehicle/edit".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                Vehicle v = findById(form.getOrDefault("id", ""));
                if (v == null) {
                    notFound(exchange);
                    return;
                }
                if (!v.uploaded) {
                    html(exchange, page(exchange, "Vehicle Locked", lockedVehicleContent(v), true));
                    return;
                }
                updateSubmission(v, form);
                redirect(exchange, "/vehicle?id=" + url(v.id));
                return;
            }
            if ("/vehicle/delete".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                String id = form.getOrDefault("id", "");
                Vehicle v = findById(id);
                if (v == null) {
                    notFound(exchange);
                    return;
                }
                if (!v.uploaded) {
                    html(exchange, page(exchange, "Vehicle Locked", lockedVehicleContent(v), true));
                    return;
                }
                deleteSubmission(id);
                redirect(exchange, "/settings?deleted=1");
                return;
            }
            if ("/settings".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String settingsHtml = Files.readString(TEMPLATES_DIR.resolve("settings/premium-settings.html"), StandardCharsets.UTF_8);
                html(exchange, settingsHtml);
                return;
            }
            if ("/settings".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                String validationError = validateSettingsContact(form);
                if (validationError != null) {
                    redirect(exchange, "/settings?error=" + url(validationError));
                    return;
                }
                saveSettings(exchange, form);
                redirect(exchange, "/settings?saved=1");
                return;
            }
            if ("/settings/action".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                handleSettingsAction(exchange, form);
                return;
            }
            if ("/settings/export".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendSettingsExport(exchange);
                return;
            }
            if ("/profile/export".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCurrentUserExport(exchange);
                return;
            }
            if ("/profile/details".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                html(exchange, page(exchange, "User Details", userDetailsContent(exchange), true));
                return;
            }
            if ("/admin/users.txt".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                ensureAdmin(exchange);
                sendAdminUsersFile(exchange);
                return;
            }
            if ("/admin/users/update".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                ensureAdmin(exchange);
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                String result = updateUserFromAdmin(form);
                redirect(exchange, "/admin?msg=" + url(result));
                return;
            }
            if ("/admin/users/status".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                ensureAdmin(exchange);
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                String username = safe(form.getOrDefault("username", form.getOrDefault("email", ""))).trim();
                String status = safe(form.getOrDefault("status", "active")).trim();
                String reason = safe(form.getOrDefault("banReason", "")).trim();
                String note = safe(form.getOrDefault("adminNote", "")).trim();
                boolean ok = updateUserStatus(username, status, reason, note);
                if (!ok) { send(exchange, 404, "text/plain; charset=utf-8", "User not found".getBytes(StandardCharsets.UTF_8)); return; }
                send(exchange, 200, "text/plain; charset=utf-8", "User status saved".getBytes(StandardCharsets.UTF_8));
                return;
            }
            if ("/profile".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                html(exchange, premiumProfilePage(exchange, false));
                return;
            }
            if ("/buyer-profile".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                html(exchange, premiumProfilePage(exchange, true));
                return;
            }
            if ("/profile".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                String message = updateCurrentUser(exchange, form);
                redirect(exchange, "/profile?msg=" + url(message));
                return;
            }
            if ("/profile/delete".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                deleteCurrentUser(exchange);
                clearSession(exchange);
                redirect(exchange, "/auth");
                return;
            }
            if ("/requests".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                html(exchange, page(exchange, "Requests", requestsContent(exchange, requestMessage(query)), true));
                return;
            }
            if ("/requests".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                String requestResult = createRequest(exchange, form);
                redirect(exchange, "/requests?msg=" + url(requestResult));
                return;
            }
            if ("/requests/action".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                handleRequestAction(exchange, form);
                return;
            }
            if ("/reviews".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                html(exchange, page(exchange, "Reviews", reviewsContent(exchange, reviewMessage(query)), true));
                return;
            }
            if ("/reviews".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                createReview(exchange, form);
                redirect(exchange, "/reviews?msg=" + url("Review saved."));
                return;
            }
            if ("/reviews/action".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                handleReviewAction(exchange, form);
                return;
            }
            if ("/admin".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                ensureAdmin(exchange);
                html(exchange, Files.readString(TEMPLATES_DIR.resolve("admin/control-panel.html"), StandardCharsets.UTF_8));
                return;
            }
            if ("/uploads".equals(path)) {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                html(exchange, page(exchange, "Recent Uploads", uploadsContent(query.getOrDefault("q", "")), true));
                return;
            }
            if ("/why-trust-us".equals(path)) {
                html(exchange, page(exchange, "Why Trust Us", whyTrustUsContent(), true));
                return;
            }
            notFound(exchange);
        } catch (Exception e) {
            e.printStackTrace();
            String body = page(exchange, "Error", "<section class='container'><div class='panel'><h1>Something went wrong</h1><p>" + esc(e.getMessage()) + "</p></div></section>", true);
            send(exchange, 500, "text/html; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
        }
    }
}
