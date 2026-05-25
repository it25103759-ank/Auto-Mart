from pathlib import Path
p=Path('src/main/java/com/automart/AutoMartApplication.java')
s=p.read_text()

s=s.replace('''    private static final Path SUBMISSIONS_FILE = DATA_DIR.resolve("submissions.tsv");
    private static final Path UPLOAD_ROOT = ROOT.resolve("uploads/vehicle-submissions");
    private static final DateTimeFormatter STORED = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
''','''    private static final Path SUBMISSIONS_FILE = DATA_DIR.resolve("submissions.tsv");
    private static final Path USERS_FILE = DATA_DIR.resolve("users.txt");
    private static final Path REQUESTS_FILE = DATA_DIR.resolve("requests.txt");
    private static final Path REVIEWS_FILE = DATA_DIR.resolve("reviews.txt");
    private static final Path UPLOAD_ROOT = ROOT.resolve("uploads/vehicle-submissions");
    private static final DateTimeFormatter STORED = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
''')

s=s.replace('''        if (!Files.exists(SUBMISSIONS_FILE) || Files.size(SUBMISSIONS_FILE) < 10) {
            Files.writeString(SUBMISSIONS_FILE, seedData(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
''','''        if (!Files.exists(SUBMISSIONS_FILE) || Files.size(SUBMISSIONS_FILE) < 10) {
            Files.writeString(SUBMISSIONS_FILE, seedData(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        seedUsers();
        seedRequests();
        seedReviews();
''')

s=s.replace('''                    if (username.isBlank() || password.isBlank()) {
                        html(exchange, authPage("Enter both username and password to continue.", false));
                        return;
                    }
                    startSession(exchange, username);
                    redirect(exchange, "/");
''','''                    if (username.isBlank() || password.isBlank()) {
                        html(exchange, authPage("Enter both username and password to continue.", false));
                        return;
                    }
                    AppUser user = authenticateUser(username, password);
                    if (user == null) {
                        html(exchange, authPage("Invalid username or password.", false));
                        return;
                    }
                    startSession(exchange, user.username);
                    redirect(exchange, "/");
''')

s=s.replace('''                    String username = safe(form.getOrDefault("username", "")).trim();
                    String password = safe(form.getOrDefault("password", "")).trim();
                    if (username.isBlank() || password.isBlank()) {
                        html(exchange, authPage("Create a username and password to enter Auto Mart.", true));
                        return;
                    }
                    startSession(exchange, username);
                    redirect(exchange, "/");
''','''                    String username = safe(form.getOrDefault("username", "")).trim();
                    String email = safe(form.getOrDefault("email", "")).trim();
                    String role = safe(form.getOrDefault("role", "buyer")).trim();
                    String password = safe(form.getOrDefault("password", "")).trim();
                    String confirmPassword = safe(form.getOrDefault("confirmPassword", "")).trim();
                    String phone = safe(form.getOrDefault("phone", "")).trim();
                    if (username.isBlank() || password.isBlank() || email.isBlank()) {
                        html(exchange, authPage("Create a username, email, and password to enter Auto Mart.", true));
                        return;
                    }
                    if (!password.equals(confirmPassword)) {
                        html(exchange, authPage("Password confirmation does not match.", true));
                        return;
                    }
                    String signUpError = registerUser(username, email, phone, password, role);
                    if (signUpError != null) {
                        html(exchange, authPage(signUpError, true));
                        return;
                    }
                    startSession(exchange, username);
                    redirect(exchange, "/");
''')

insert_after='''                if ("/settings/export".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendSettingsExport(exchange);
                    return;
                }
'''
add_routes='''                if ("/profile".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                    html(exchange, page("com.automart.Profile", profileContent(exchange, profileMessage(query)), true));
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
                    html(exchange, page("Requests", requestsContent(exchange, requestMessage(query)), true));
                    return;
                }
                if ("/requests".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                    createRequest(exchange, form);
                    redirect(exchange, "/requests?msg=" + url("Request submitted."));
                    return;
                }
                if ("/requests/action".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> form = parseForm(exchange.getRequestBody().readAllBytes());
                    handleRequestAction(exchange, form);
                    return;
                }
                if ("/reviews".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                    html(exchange, page("Reviews", reviewsContent(exchange, reviewMessage(query)), true));
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
                    html(exchange, page("Admin Dashboard", adminContent(), true));
                    return;
                }
'''
s=s.replace(insert_after, insert_after+add_routes)

s=s.replace('''        String form = "<form class='auth-form' method='post' action='" + formAction + "'>"
                + "<label><span>Username</span><input name='username' placeholder='Enter your username' required></label>"
                + (signUp ? "<label><span>Email address</span><input name='email' type='email' placeholder='name@example.com'></label>" : "")
                + "<label><span>Password</span><input name='password' type='password' placeholder='Enter your password' required></label>"
                + (signUp ? "<label><span>Confirm password</span><input name='confirmPassword' type='password' placeholder='Confirm your password'></label>" : "")
                + "<button class='btn btn-primary auth-btn' type='submit'>" + cta + "</button>"
                + "</form>";
''','''        String form = "<form class='auth-form' method='post' action='" + formAction + "'>"
                + "<label><span>Username</span><input name='username' placeholder='Enter your username' required></label>"
                + (signUp ? "<label><span>Email address</span><input name='email' type='email' placeholder='name@example.com' required></label>" : "")
                + (signUp ? "<label><span>Phone number</span><input name='phone' placeholder='+94 77 123 4567'></label>" : "")
                + (signUp ? "<label><span>Account role</span><select name='role'><option value='buyer'>Buyer</option><option value='seller'>Seller</option></select></label>" : "")
                + "<label><span>Password</span><input name='password' type='password' placeholder='Enter your password' required></label>"
                + (signUp ? "<label><span>Confirm password</span><input name='confirmPassword' type='password' placeholder='Confirm your password' required></label>" : "")
                + "<button class='btn btn-primary auth-btn' type='submit'>" + cta + "</button>"
                + "</form>";
''')

s=s.replace('''    private static String header() {
        return "<header class='site-header'><div class='container nav'><a class='brand' href='/'><img src='/assets/img/auto-mart-logo.svg' alt='Auto Mart logo'><div><strong>Auto Mart</strong><span>Premium second-hand cars in Sri Lanka</span></div></a><nav class='top-nav'><a class='top-nav-link' href='/'><span>Home</span></a><a class='top-nav-link' href='/inventory'><span>Inventory</span></a><a class='top-nav-link' href='/sell'><span>Sell Your Car</span></a><a class='top-nav-link' href='/uploads'><span>Recent Uploads</span></a><a class='top-nav-link' href='/why-trust-us'><span>Why Trust Us</span></a><a class='top-nav-link' href='/settings'><span>Settings</span></a><a class='logout-link' href='/logout'><span>Logout</span></a></nav></div></header>";
    }
''','''    private static String header() {
        return "<header class='site-header'><div class='container nav'><a class='brand' href='/'><img src='/assets/img/auto-mart-logo.svg' alt='Auto Mart logo'><div><strong>Auto Mart</strong><span>Premium second-hand cars in Sri Lanka</span></div></a><nav class='top-nav'><a class='top-nav-link' href='/'><span>Home</span></a><a class='top-nav-link' href='/inventory'><span>Inventory</span></a><a class='top-nav-link' href='/sell'><span>Sell Your Car</span></a><a class='top-nav-link' href='/requests'><span>Requests</span></a><a class='top-nav-link' href='/reviews'><span>Reviews</span></a><a class='top-nav-link' href='/profile'><span>com.automart.Profile</span></a><a class='top-nav-link' href='/uploads'><span>Recent Uploads</span></a><a class='top-nav-link' href='/settings'><span>Settings</span></a>" + adminLink() + "<a class='logout-link' href='/logout'><span>Logout</span></a></nav></div></header>";
    }
''')

s=s.replace('''    private static boolean isAuthenticated(HttpExchange exchange) {
        return !readCookie(exchange, AUTH_COOKIE).isBlank();
    }
''','''    private static boolean isAuthenticated(HttpExchange exchange) {
        return currentUser(exchange) != null;
    }
''')

s=s.replace('''    private static void sortVehicles(List<Vehicle> vehicles, String sort) {
        switch (sort) {
            case "price-low" -> vehicles.sort(Comparator.comparingLong(v -> parsePriceValue(v.price)));
            case "price-high" -> vehicles.sort(Comparator.comparingLong((Vehicle v) -> parsePriceValue(v.price)).reversed());
            case "year-new" -> vehicles.sort(Comparator.comparingInt((Vehicle v) -> parseYearValue(v.year)).reversed());
            case "year-old" -> vehicles.sort(Comparator.comparingInt(v -> parseYearValue(v.year)));
            default -> vehicles.sort(Comparator.comparing((Vehicle v) -> v.createdAt).reversed());
        }
    }
''','''    private static void sortVehicles(List<Vehicle> vehicles, String sort) {
        switch (sort) {
            case "price-low" -> vehicles.clear();
            case "price-high" -> vehicles.clear();
            case "year-new" -> vehicles.sort(Comparator.comparingInt((Vehicle v) -> parseYearValue(v.year)).reversed());
            case "year-old" -> vehicles.sort(Comparator.comparingInt(v -> parseYearValue(v.year)));
            default -> vehicles.sort(Comparator.comparing((Vehicle v) -> v.createdAt).reversed());
        }
        if ("price-low".equals(sort) || "price-high".equals(sort)) {
            VehicleLinkedList linkedList = new VehicleLinkedList();
            for (Vehicle vehicle : new ArrayList<>(loadVehiclesSafeForSort(vehicles))) {
                linkedList.insert(vehicle);
            }
            List<Vehicle> sorted = linkedList.mergeSortByPrice("price-high".equals(sort));
            vehicles.clear();
            vehicles.addAll(sorted);
        }
    }
''')

# insert helper loadVehiclesSafeForSort after parseYearValue
s=s.replace('''    private static int parseYearValue(String raw) {
        try {
            return Integer.parseInt(safe(raw).replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
''','''    private static int parseYearValue(String raw) {
        try {
            return Integer.parseInt(safe(raw).replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private static List<Vehicle> loadVehiclesSafeForSort(List<Vehicle> vehicles) {
        return new ArrayList<>(vehicles);
    }
''')

marker='''    private static final class Settings {
'''
insert='''
    private static String adminLink() {
        return "<a class='top-nav-link' href='/admin'><span>Admin</span></a>";
    }

    private static String profileMessage(Map<String, String> query) {
        return safe(query.getOrDefault("msg", ""));
    }

    private static String requestMessage(Map<String, String> query) {
        return safe(query.getOrDefault("msg", ""));
    }

    private static String reviewMessage(Map<String, String> query) {
        return safe(query.getOrDefault("msg", ""));
    }

    private static String profileContent(HttpExchange exchange, String message) throws IOException {
        AppUser user = currentUser(exchange);
        if (user == null) {
            return "<section class='container section'><div class='panel'><h1>No active user</h1></div></section>";
        }
        String alert = message.isBlank() ? "" : "<div class='alert'>" + esc(message) + "</div>";
        String roleNote = user.roleMessage();
        return "<section class='container section'><p class='eyebrow'>USER MANAGEMENT</p><h1>com.automart.Profile Management</h1>" + alert
                + "<div class='panel'><p>" + esc(roleNote) + "</p><form class='sell-form' method='post' action='/profile'>"
                + fieldWithValue("Username", "username", "username", user.getUsername())
                + fieldWithValue("Email", "email", "name@example.com", user.getEmail())
                + fieldWithValue("Phone", "phone", "+94 77 123 4567", user.getPhone())
                + fieldWithValue("Password", "password", "Enter new password", user.getPassword())
                + "<button class='btn btn-primary' type='submit'>Update com.automart.Profile</button></form>"
                + "<form method='post' action='/profile/delete' onsubmit=\"return confirm('Delete this account?');\"><button class='btn btn-danger' type='submit'>Delete Account</button></form></div></section>";
    }

    private static String requestsContent(HttpExchange exchange, String message) throws IOException {
        List<Vehicle> vehicles = loadVehicles();
        List<PurchaseRequest> requests = loadRequests();
        StringBuilder vehicleOptions = new StringBuilder();
        for (Vehicle vehicle : vehicles.subList(0, Math.min(20, vehicles.size()))) {
            vehicleOptions.append("<option value='").append(esc(vehicle.id)).append("'>").append(esc(vehicle.title)).append("</option>");
        }
        StringBuilder rows = new StringBuilder();
        for (PurchaseRequest request : requests) {
            rows.append("<tr><td>").append(esc(request.id)).append("</td><td>").append(esc(request.vehicleTitle)).append("</td><td>").append(esc(request.buyerUsername)).append("</td><td>").append(esc(request.status)).append("</td><td>")
                    .append("<form method='post' action='/requests/action' class='inline-form'>")
                    .append(hiddenField("id", request.id))
                    .append("<select name='status'><option>Pending</option><option>Approved</option><option>Rejected</option></select>")
                    .append("<button class='btn btn-secondary' type='submit' name='action' value='update'>Save</button>")
                    .append("<button class='btn btn-danger' type='submit' name='action' value='delete'>Delete</button></form></td></tr>");
        }
        if (rows.length() == 0) rows.append("<tr><td colspan='5'>No requests yet.</td></tr>");
        String alert = message.isBlank() ? "" : "<div class='alert'>" + esc(message) + "</div>";
        return "<section class='container section'><p class='eyebrow'>BUYING & REQUEST MANAGEMENT</p><h1>Purchase Requests</h1>" + alert
                + "<div class='panel'><form class='sell-form' method='post' action='/requests'>"
                + "<label>Vehicle<select name='vehicleId'>" + vehicleOptions + "</select></label>"
                + textareaField("Request note", "note", "I would like to inspect this car this weekend.")
                + "<button class='btn btn-primary' type='submit'>Send Request</button></form></div>"
                + "<div class='panel'><table><thead><tr><th>ID</th><th>Vehicle</th><th>Buyer</th><th>Status</th><th>Actions</th></tr></thead><tbody>" + rows + "</tbody></table></div></section>";
    }

    private static String reviewsContent(HttpExchange exchange, String message) throws IOException {
        List<Vehicle> vehicles = loadVehicles();
        List<ReviewEntry> reviews = loadReviews();
        StringBuilder vehicleOptions = new StringBuilder();
        for (Vehicle vehicle : vehicles.subList(0, Math.min(20, vehicles.size()))) {
            vehicleOptions.append("<option value='").append(esc(vehicle.id)).append("'>").append(esc(vehicle.title)).append("</option>");
        }
        StringBuilder cards = new StringBuilder();
        for (ReviewEntry review : reviews) {
            cards.append("<article class='panel'><h3>").append(esc(review.vehicleTitle)).append(" · ").append(esc(review.rating)).append("/5</h3><p><strong>").append(esc(review.authorUsername)).append("</strong> — ").append(esc(review.type)).append("</p><p>").append(esc(review.comment)).append("</p>")
                 .append("<form method='post' action='/reviews/action' class='inline-form'>")
                 .append(hiddenField("id", review.id))
                 .append("<input name='comment' value='").append(esc(review.comment)).append("'>")
                 .append("<button class='btn btn-secondary' type='submit' name='action' value='update'>Update</button>")
                 .append("<button class='btn btn-danger' type='submit' name='action' value='delete'>Delete</button></form></article>");
        }
        if (cards.length() == 0) cards.append("<div class='panel'>No reviews yet.</div>");
        String alert = message.isBlank() ? "" : "<div class='alert'>" + esc(message) + "</div>";
        return "<section class='container section'><p class='eyebrow'>FEEDBACK & REVIEW SYSTEM</p><h1>Seller and Vehicle Reviews</h1>" + alert
                + "<div class='panel'><form class='sell-form' method='post' action='/reviews'>"
                + "<label>Vehicle<select name='vehicleId'>" + vehicleOptions + "</select></label>"
                + "<label>Rating<select name='rating'><option>5</option><option>4</option><option>3</option><option>2</option><option>1</option></select></label>"
                + "<label>Review type<select name='type'><option>VerifiedReview</option><option>PublicReview</option></select></label>"
                + textareaField("Comment", "comment", "Great communication and a clean vehicle.")
                + "<button class='btn btn-primary' type='submit'>Submit Review</button></form></div><div class='grid cards'>" + cards + "</div></section>";
    }

    private static String adminContent() throws IOException {
        List<AppUser> users = loadUsers();
        List<Vehicle> vehicles = loadVehicles();
        List<PurchaseRequest> requests = loadRequests();
        List<ReviewEntry> reviews = loadReviews();
        StringBuilder userRows = new StringBuilder();
        for (AppUser user : users) {
            userRows.append("<tr><td>").append(esc(user.getUsername())).append("</td><td>").append(esc(user.getRole())).append("</td><td>").append(esc(user.getEmail())).append("</td><td>").append(esc(user.getPhone())).append("</td></tr>");
        }
        return "<section class='container section'><p class='eyebrow'>ADMIN DASHBOARD</p><h1>System Monitoring</h1>"
                + "<div class='grid cards'>"
                + "<article class='panel'><h3>Users</h3><p>" + users.size() + " registered accounts</p></article>"
                + "<article class='panel'><h3>Listings</h3><p>" + vehicles.size() + " vehicles stored</p></article>"
                + "<article class='panel'><h3>Requests</h3><p>" + requests.size() + " purchase requests</p></article>"
                + "<article class='panel'><h3>Reviews</h3><p>" + reviews.size() + " feedback entries</p></article></div>"
                + "<div class='panel'><h2>User Management</h2><table><thead><tr><th>Username</th><th>Role</th><th>Email</th><th>Phone</th></tr></thead><tbody>" + userRows + "</tbody></table></div></section>";
    }

    private static void ensureAdmin(HttpExchange exchange) {
        AppUser user = currentUser(exchange);
        if (user == null || !user.isAdmin()) {
            throw new IllegalStateException("Admin access only. Sign in as admin / admin123.");
        }
    }

    private static AppUser currentUser(HttpExchange exchange) {
        String username = safe(readCookie(exchange, AUTH_COOKIE)).trim();
        if (username.isBlank()) {
            return null;
        }
        try {
            return findUser(username);
        } catch (IOException e) {
            return null;
        }
    }

    private static void seedUsers() throws IOException {
        if (Files.exists(USERS_FILE) && Files.size(USERS_FILE) > 0) return;
        List<String> lines = List.of(
                new AdminUser("admin", "admin@automart.lk", "+94 11 000 0000", "admin123").toRecord(),
                new BuyerUser("buyer1", "buyer1@automart.lk", "+94 77 000 0001", "buyer123").toRecord(),
                new SellerUser("seller1", "seller1@automart.lk", "+94 77 000 0002", "seller123").toRecord()
        );
        Files.write(USERS_FILE, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void seedRequests() throws IOException {
        if (!Files.exists(REQUESTS_FILE)) Files.writeString(REQUESTS_FILE, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    }

    private static void seedReviews() throws IOException {
        if (!Files.exists(REVIEWS_FILE)) Files.writeString(REVIEWS_FILE, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    }

    private static List<AppUser> loadUsers() throws IOException {
        List<AppUser> users = new ArrayList<>();
        if (!Files.exists(USERS_FILE)) return users;
        for (String line : Files.readAllLines(USERS_FILE, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            AppUser user = AppUser.fromRecord(line);
            if (user != null) users.add(user);
        }
        return users;
    }

    private static AppUser findUser(String username) throws IOException {
        for (AppUser user : loadUsers()) {
            if (user.getUsername().equalsIgnoreCase(safe(username).trim())) return user;
        }
        return null;
    }

    private static AppUser authenticateUser(String username, String password) throws IOException {
        AppUser user = findUser(username);
        return user != null && user.checkPassword(password) ? user : null;
    }

    private static String registerUser(String username, String email, String phone, String password, String role) throws IOException {
        if (findUser(username) != null) return "That username is already taken.";
        AppUser user = switch (safe(role).toLowerCase(Locale.ROOT)) {
            case "seller" -> new SellerUser(username, email, phone, password);
            default -> new BuyerUser(username, email, phone, password);
        };
        Files.writeString(USERS_FILE, user.toRecord() + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return null;
    }

    private static String updateCurrentUser(HttpExchange exchange, Map<String, String> form) throws IOException {
        AppUser current = currentUser(exchange);
        if (current == null) return "No active user found.";
        List<AppUser> users = loadUsers();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equalsIgnoreCase(current.getUsername())) {
                users.get(i).setUsername(safe(form.getOrDefault("username", current.getUsername())).trim());
                users.get(i).setEmail(safe(form.getOrDefault("email", current.getEmail())).trim());
                users.get(i).setPhone(safe(form.getOrDefault("phone", current.getPhone())).trim());
                users.get(i).setPassword(safe(form.getOrDefault("password", current.getPassword())).trim());
                Files.write(USERS_FILE, users.stream().map(AppUser::toRecord).toList(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                startSession(exchange, users.get(i).getUsername());
                return "com.automart.Profile updated successfully.";
            }
        }
        return "com.automart.Profile update failed.";
    }

    private static void deleteCurrentUser(HttpExchange exchange) throws IOException {
        AppUser current = currentUser(exchange);
        if (current == null) return;
        List<String> lines = new ArrayList<>();
        for (AppUser user : loadUsers()) {
            if (!user.getUsername().equalsIgnoreCase(current.getUsername())) lines.add(user.toRecord());
        }
        Files.write(USERS_FILE, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static List<PurchaseRequest> loadRequests() throws IOException {
        List<PurchaseRequest> out = new ArrayList<>();
        if (!Files.exists(REQUESTS_FILE)) return out;
        for (String line : Files.readAllLines(REQUESTS_FILE, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            PurchaseRequest request = PurchaseRequest.fromRecord(line);
            if (request != null) out.add(request);
        }
        return out;
    }

    private static void saveAllRequests(List<PurchaseRequest> requests) throws IOException {
        Files.write(REQUESTS_FILE, requests.stream().map(PurchaseRequest::toRecord).toList(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void createRequest(HttpExchange exchange, Map<String, String> form) throws IOException {
        AppUser user = currentUser(exchange);
        Vehicle vehicle = findById(form.getOrDefault("vehicleId", ""));
        if (user == null || vehicle == null) return;
        PurchaseRequest request = new PurchaseRequest("REQ-" + System.currentTimeMillis(), vehicle.id, vehicle.title, user.getUsername(), safe(form.getOrDefault("note", "")), "Pending", LocalDateTime.now().format(STORED));
        Files.writeString(REQUESTS_FILE, request.toRecord() + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void handleRequestAction(HttpExchange exchange, Map<String, String> form) throws IOException {
        List<PurchaseRequest> requests = loadRequests();
        String id = safe(form.getOrDefault("id", ""));
        String action = safe(form.getOrDefault("action", ""));
        if ("delete".equals(action)) {
            requests.removeIf(r -> r.id.equals(id));
            saveAllRequests(requests);
            redirect(exchange, "/requests?msg=" + url("Request deleted."));
            return;
        }
        for (PurchaseRequest request : requests) {
            if (request.id.equals(id)) request.status = safe(form.getOrDefault("status", request.status));
        }
        saveAllRequests(requests);
        redirect(exchange, "/requests?msg=" + url("Request status updated."));
    }

    private static List<ReviewEntry> loadReviews() throws IOException {
        List<ReviewEntry> out = new ArrayList<>();
        if (!Files.exists(REVIEWS_FILE)) return out;
        for (String line : Files.readAllLines(REVIEWS_FILE, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            ReviewEntry review = ReviewEntry.fromRecord(line);
            if (review != null) out.add(review);
        }
        return out;
    }

    private static void saveAllReviews(List<ReviewEntry> reviews) throws IOException {
        Files.write(REVIEWS_FILE, reviews.stream().map(ReviewEntry::toRecord).toList(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void createReview(HttpExchange exchange, Map<String, String> form) throws IOException {
        AppUser user = currentUser(exchange);
        Vehicle vehicle = findById(form.getOrDefault("vehicleId", ""));
        if (user == null || vehicle == null) return;
        ReviewEntry review = ReviewEntry.create(safe(form.getOrDefault("type", "PublicReview")), "REV-" + System.currentTimeMillis(), vehicle.id, vehicle.title, user.getUsername(), safe(form.getOrDefault("comment", "")), safe(form.getOrDefault("rating", "5")), LocalDateTime.now().format(STORED));
        Files.writeString(REVIEWS_FILE, review.toRecord() + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void handleReviewAction(HttpExchange exchange, Map<String, String> form) throws IOException {
        List<ReviewEntry> reviews = loadReviews();
        String id = safe(form.getOrDefault("id", ""));
        String action = safe(form.getOrDefault("action", ""));
        if ("delete".equals(action)) {
            reviews.removeIf(r -> r.id.equals(id));
            saveAllReviews(reviews);
            redirect(exchange, "/reviews?msg=" + url("Review deleted."));
            return;
        }
        for (ReviewEntry review : reviews) {
            if (review.id.equals(id)) review.comment = safe(form.getOrDefault("comment", review.comment));
        }
        saveAllReviews(reviews);
        redirect(exchange, "/reviews?msg=" + url("Review updated."));
    }

    private static abstract class AppUser {
        private String username;
        private String email;
        private String phone;
        private String password;
        private final String role;

        AppUser(String username, String email, String phone, String password, String role) {
            this.username = safe(username).trim();
            this.email = safe(email).trim();
            this.phone = safe(phone).trim();
            this.password = safe(password).trim();
            this.role = role;
        }

        String getUsername() { return username; }
        String getEmail() { return email; }
        String getPhone() { return phone; }
        String getPassword() { return password; }
        String getRole() { return role; }
        void setUsername(String username) { this.username = safe(username).trim(); }
        void setEmail(String email) { this.email = safe(email).trim(); }
        void setPhone(String phone) { this.phone = safe(phone).trim(); }
        void setPassword(String password) { this.password = safe(password).trim(); }
        boolean checkPassword(String candidate) { return safe(password).equals(safe(candidate)); }
        boolean isAdmin() { return "admin".equalsIgnoreCase(role); }
        String toRecord() { return String.join("\t", role, Vehicle.clean(username), Vehicle.clean(email), Vehicle.clean(phone), Vehicle.clean(password)); }
        static AppUser fromRecord(String line) {
            String[] p = line.split("\\t", -1);
            if (p.length < 5) return null;
            return switch (p[0].toLowerCase(Locale.ROOT)) {
                case "admin" -> new AdminUser(p[1], p[2], p[3], p[4]);
                case "seller" -> new SellerUser(p[1], p[2], p[3], p[4]);
                default -> new BuyerUser(p[1], p[2], p[3], p[4]);
            };
        }
        abstract String roleMessage();
    }

    private static final class BuyerUser extends AppUser {
        BuyerUser(String username, String email, String phone, String password) { super(username, email, phone, password, "buyer"); }
        @Override String roleMessage() { return "Buyer profile: use this account to request vehicles and leave reviews."; }
    }

    private static final class SellerUser extends AppUser {
        SellerUser(String username, String email, String phone, String password) { super(username, email, phone, password, "seller"); }
        @Override String roleMessage() { return "Seller profile: manage listings, approve requests, and respond to buyers."; }
    }

    private static final class AdminUser extends AppUser {
        AdminUser(String username, String email, String phone, String password) { super(username, email, phone, password, "admin"); }
        @Override String roleMessage() { return "Admin profile: monitor users, listings, requests, and reviews from the dashboard."; }
    }

    private static final class PurchaseRequest {
        final String id;
        final String vehicleId;
        final String vehicleTitle;
        final String buyerUsername;
        final String note;
        String status;
        final String createdAt;
        PurchaseRequest(String id, String vehicleId, String vehicleTitle, String buyerUsername, String note, String status, String createdAt) {
            this.id=id; this.vehicleId=vehicleId; this.vehicleTitle=vehicleTitle; this.buyerUsername=buyerUsername; this.note=note; this.status=status; this.createdAt=createdAt;
        }
        String toRecord() { return String.join("\t", id, vehicleId, Vehicle.clean(vehicleTitle), Vehicle.clean(buyerUsername), Vehicle.clean(note), status, createdAt); }
        static PurchaseRequest fromRecord(String line) { String[] p=line.split("\\t",-1); return p.length<7?null:new PurchaseRequest(p[0],p[1],p[2],p[3],p[4],p[5],p[6]); }
    }

    private static class ReviewEntry {
        final String id;
        final String vehicleId;
        final String vehicleTitle;
        final String authorUsername;
        String comment;
        final String rating;
        final String createdAt;
        final String type;
        ReviewEntry(String id, String vehicleId, String vehicleTitle, String authorUsername, String comment, String rating, String createdAt, String type) {
            this.id=id; this.vehicleId=vehicleId; this.vehicleTitle=vehicleTitle; this.authorUsername=authorUsername; this.comment=comment; this.rating=rating; this.createdAt=createdAt; this.type=type;
        }
        static ReviewEntry create(String type, String id, String vehicleId, String vehicleTitle, String authorUsername, String comment, String rating, String createdAt) {
            return "VerifiedReview".equalsIgnoreCase(type) ? new VerifiedReview(id, vehicleId, vehicleTitle, authorUsername, comment, rating, createdAt) : new PublicReview(id, vehicleId, vehicleTitle, authorUsername, comment, rating, createdAt);
        }
        String toRecord() { return String.join("\t", type, id, vehicleId, Vehicle.clean(vehicleTitle), Vehicle.clean(authorUsername), Vehicle.clean(comment), rating, createdAt); }
        static ReviewEntry fromRecord(String line) { String[] p=line.split("\\t",-1); return p.length<8?null:create(p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7]); }
    }

    private static final class VerifiedReview extends ReviewEntry {
        VerifiedReview(String id, String vehicleId, String vehicleTitle, String authorUsername, String comment, String rating, String createdAt) { super(id, vehicleId, vehicleTitle, authorUsername, comment, rating, createdAt, "VerifiedReview"); }
    }

    private static final class PublicReview extends ReviewEntry {
        PublicReview(String id, String vehicleId, String vehicleTitle, String authorUsername, String comment, String rating, String createdAt) { super(id, vehicleId, vehicleTitle, authorUsername, comment, rating, createdAt, "PublicReview"); }
    }

    private static final class VehicleNode {
        Vehicle data;
        VehicleNode next;
        VehicleNode(Vehicle data) { this.data = data; }
    }

    private static final class VehicleLinkedList {
        private VehicleNode head;
        void insert(Vehicle vehicle) {
            VehicleNode node = new VehicleNode(vehicle);
            if (head == null) { head = node; return; }
            VehicleNode current = head;
            while (current.next != null) current = current.next;
            current.next = node;
        }
        List<Vehicle> mergeSortByPrice(boolean descending) {
            head = mergeSort(head, descending);
            List<Vehicle> sorted = new ArrayList<>();
            VehicleNode current = head;
            while (current != null) { sorted.add(current.data); current = current.next; }
            return sorted;
        }
        private VehicleNode mergeSort(VehicleNode node, boolean descending) {
            if (node == null || node.next == null) return node;
            VehicleNode middle = split(node);
            VehicleNode left = mergeSort(node, descending);
            VehicleNode right = mergeSort(middle, descending);
            return merge(left, right, descending);
        }
        private VehicleNode split(VehicleNode head) {
            VehicleNode slow = head, fast = head.next;
            while (fast != null && fast.next != null) { slow = slow.next; fast = fast.next.next; }
            VehicleNode second = slow.next;
            slow.next = null;
            return second;
        }
        private VehicleNode merge(VehicleNode a, VehicleNode b, boolean descending) {
            if (a == null) return b;
            if (b == null) return a;
            boolean takeA = descending ? parsePriceValue(a.data.price) >= parsePriceValue(b.data.price) : parsePriceValue(a.data.price) <= parsePriceValue(b.data.price);
            if (takeA) { a.next = merge(a.next, b, descending); return a; }
            b.next = merge(a, b.next, descending); return b;
        }
    }

'''
s=s.replace(marker, insert+marker)
p.write_text(s)
print('patched')
