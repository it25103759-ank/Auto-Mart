import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
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
import java.util.regex.Pattern;
import java.util.concurrent.Executors;

public class AutoMartApplication {
    static final int PORT = Integer
            .parseInt(System.getProperty("automart.port", System.getenv().getOrDefault("AUTOMART_PORT", "8080")));
    static final Path ROOT = Paths.get("").toAbsolutePath();
    static final Path STATIC_DIR = ROOT.resolve("src/main/resources/static");
    static final Path TEMPLATES_DIR = ROOT.resolve("src/main/resources/templates");
    static final Path DATA_DIR = ROOT.resolve("src/main/resources/data");
    static final Path SUBMISSIONS_FILE = DATA_DIR.resolve("submissions.tsv");
    static final Path USERS_FILE = DATA_DIR.resolve("users.txt");
    static final Path REQUESTS_FILE = DATA_DIR.resolve("requests.txt");
    static final Path REVIEWS_FILE = DATA_DIR.resolve("reviews.txt");
    static final Path LOGS_FILE = DATA_DIR.resolve("logs.txt");
    static final Path UPLOAD_ROOT = ROOT.resolve("uploads/vehicle-submissions");
    static final DateTimeFormatter STORED = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    static final DateTimeFormatter SHOW = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    static final String AUTH_COOKIE = "automart_session";
    static final String SETTINGS_COOKIE = "automart_settings";
    static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-z0-9._]+@gmail\\.com$");
    static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");
    static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    public static void main(String[] args) throws Exception {
        bootstrap();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new AppHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Auto Mart running at http://localhost:" + PORT);
    }

    static void bootstrap() throws IOException {
        Files.createDirectories(DATA_DIR);
        Files.createDirectories(UPLOAD_ROOT);
        for (String c : categories()) {
            Files.createDirectories(UPLOAD_ROOT.resolve(c));
        }
        if (!Files.exists(SUBMISSIONS_FILE) || Files.size(SUBMISSIONS_FILE) < 10) {
            Files.writeString(SUBMISSIONS_FILE, seedData(), StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }
        seedUsers();
        seedRequests();
        seedReviews();
        if (!Files.exists(LOGS_FILE))
            Files.writeString(LOGS_FILE, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    }

    static Vehicle saveSubmission(Map<String, String> form) throws IOException {
        String category = normalizeCategory(form.getOrDefault("category", "suv"));
        String title = safe(form.getOrDefault("title", "Untitled Vehicle"));
        String price = normalizePriceLkr(form.getOrDefault("price", "0"));
        String year = safe(form.getOrDefault("year", "2020"));
        String mileage = safe(form.getOrDefault("mileage", "0 km"));
        String fuel = safe(form.getOrDefault("fuel", "Petrol"));
        String transmission = safe(form.getOrDefault("transmission", "Automatic"));
        String description = safe(
                form.getOrDefault("description", "Well-kept second-hand vehicle with strong value and road presence."));
        String image = normalizeSubmittedImage(category, safe(form.getOrDefault("image", defaultImage(category))));
        String id = slug(title) + "-" + System.currentTimeMillis();
        String time = LocalDateTime.now().format(STORED);
        Vehicle v = new Vehicle(id, category, title, price, year, mileage, fuel, transmission, description, image, time,
                true);
        Files.writeString(SUBMISSIONS_FILE, v.toTsv() + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        Path categoryDir = UPLOAD_ROOT.resolve(category);
        Files.createDirectories(categoryDir);
        Files.writeString(categoryDir.resolve(id + ".txt"), v.toText(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return v;
    }

    static Vehicle findById(String id) throws IOException {
        for (Vehicle v : loadVehicles()) {
            if (v.id.equals(id)) {
                return v;
            }
        }
        return null;
    }

    static List<Vehicle> loadVehicles() throws IOException {
        List<Vehicle> out = FileHelper.readVehicles();
        out.sort(Comparator.comparing((Vehicle v) -> v.createdAt).reversed());
        return out;
    }

    static String homeContent(HttpExchange exchange) throws IOException {
        List<Vehicle> all = loadVehicles();
        AppUser user = currentUser(exchange);
        StringBuilder featured = new StringBuilder();
        for (Vehicle v : all.subList(0, Math.min(4, all.size())))
            featured.append(card(v));
        StringBuilder arrivals = new StringBuilder();
        for (Vehicle v : all.subList(0, Math.min(8, all.size())))
            arrivals.append(card(v));
        String dashboard = roleDashboard(user);
        return "<section class='hero premium-hero'>"
                + "<video class='hero-video' autoplay muted loop playsinline><source src='/assets/video/hero.mp4' type='video/mp4'></video>"
                + "<div class='hero-overlay'></div>"
                + "<div class='container hero-grid premium-hero-grid'><div class='hero-copy'>"
                + "<p class='eyebrow'>PREMIUM SECOND-HAND VEHICLES</p>"
                + "<h1>Find Your Next Premium Vehicle with Confidence</h1>"
                + "<p class='lead'>Verified listings, transparent pricing, trusted sellers, and a smooth buying experience.</p>"
                + "<div class='hero-chip-row'><span>Verified Sellers</span><span>Inspection Checked</span><span>Easy Financing</span><span>Secure Requests</span></div>"
                + "<div class='actions'><a class='btn btn-primary' href='/inventory'>Browse Inventory</a><a class='btn btn-secondary' href='/sell'>Sell Your Car</a></div>"
                + "<form class='hero-search-bar panel' method='get' action='/inventory'>"
                + "<label><span>Brand</span><input name='q' placeholder='BMW, Toyota, Audi'></label>"
                + "<label><span>Model</span><input name='model' placeholder='Model or keyword'></label>"
                + "<label><span>Price Range</span><select name='sort'><option value='latest'>Any Range</option><option value='price-low'>Budget First</option><option value='price-high'>Premium First</option></select></label>"
                + "<label><span>Body Type</span><select name='category'><option value='all'>All Types</option>"
                + optionsForCategory("all") + "</select></label>"
                + "<button class='btn btn-primary hero-search-btn' type='submit'>Search</button></form>"
                + "</div><div class='hero-side panel premium-visual-card'>"
                + "<p class='eyebrow'>AUTO MART SELECT</p><h3>Luxury presentation with practical search</h3><p>Use premium search, compare styles, track requests, and move faster from discovery to decision.</p>"
                + "<div class='hero-side-metrics'><div><strong>250+</strong><span>Verified listings</span></div><div><strong>98%</strong><span>Seller response</span></div><div><strong>24/7</strong><span>Request tracking</span></div></div>"
                + "</div></div></section>"
                + dashboard
                + "<section class='container section'><div class='section-head'><div><p class='eyebrow'>FEATURED VEHICLES</p><h2>Premium picks curated for serious buyers</h2></div><a href='/inventory'>Browse all</a></div><div class='grid cards'>"
                + featured + "</div></section>"
                + "<section class='container section premium-category-grid'><div class='section-head'><div><p class='eyebrow'>BROWSE BY CATEGORY</p><h2>Explore the right body style faster</h2></div></div><div class='topic-grid'>"
                + categoryTopic("SUV", "High-riding premium family comfort", "/inventory?category=suv")
                + categoryTopic("Sedan", "Executive comfort and refined road feel", "/inventory?category=sedan")
                + categoryTopic("Crossover", "Urban versatility with premium styling", "/inventory?category=crossover")
                + categoryTopic("Hybrid", "Efficient performance with lower running cost", "/inventory?category=hybrid")
                + "</div></section>"
                + "<section class='container section premium-trust-grid'><div class='section-head'><div><p class='eyebrow'>WHY TRUST AUTO MART</p><h2>Built for buyer confidence</h2></div><a href='/why-trust-us'>Learn more</a></div><div class='topic-grid'>"
                + topicCard("Verified Sellers",
                        "Seller identities, listings, and response activity are clearly presented.")
                + topicCard("Vehicle Condition Badges",
                        "Highlight inspected, verified, and premium-condition listings instantly.")
                + topicCard("Transparent Pricing",
                        "Clean LKR pricing with clear value positioning and premium listing quality.")
                + topicCard("Secure Requests", "Track buyer requests, approvals, and conversation flow in one place.")
                + "</div></section>"
                + "<section class='container section'><div class='section-head'><div><p class='eyebrow'>NEW ARRIVALS</p><h2>Freshly uploaded vehicles</h2></div><a href='/uploads'>Recent Uploads</a></div><div class='grid cards'>"
                + arrivals + "</div></section>"
                + "<section class='container section'><div class='section-head'><div><p class='eyebrow'>CUSTOMER REVIEWS</p><h2>Marketplace feedback that builds trust</h2></div><a href='/reviews'>View all reviews</a></div><div class='topic-grid'>"
                + reviewSnippet("Premium delivery experience",
                        "Smooth process, honest vehicle description, and fast seller response.")
                + reviewSnippet("Great inspection transparency",
                        "The listing details and condition notes made decision-making much easier.")
                + reviewSnippet("Professional seller communication",
                        "Request flow felt structured, secure, and easy to track.")
                + "</div></section>"
                + "<section class='container section premium-cta'><div class='panel cta-panel'><p class='eyebrow'>SELL YOUR CAR</p><h2>List your vehicle with premium presentation</h2><p>Upload faster, manage requests, and position your vehicle for better leads.</p><div class='actions'><a class='btn btn-primary' href='/sell'>Start Selling</a><a class='btn btn-secondary' href='/settings'>Open Seller Workspace</a></div></div></section>"
                + "<script>"
                + "document.addEventListener('DOMContentLoaded', function() {"
                + "  function fixHomeAvatar() {"
                + "    var el = document.getElementById('globalProfileAvatar');"
                + "    if(!el) return;"
                + "    var role = (document.getElementById('globalProfileRole') ? document.getElementById('globalProfileRole').textContent : '').toLowerCase();"
                + "    var photo = localStorage.getItem('automartMediaAvatarV2') || localStorage.getItem('automartMediaAvatarV1') || '';"
                + "    var pStr = localStorage.getItem('automartUserProfileRealV4');"
                + "    var bStr = localStorage.getItem('automartBuyerProfileRealV1');"
                + "    var aStr = localStorage.getItem('automartAdminProfileRealV2');"
                + "    var p = pStr ? JSON.parse(pStr) : {};"
                + "    if(role.includes('admin') && aStr) { var a = JSON.parse(aStr); if(a.photo) photo = a.photo; }"
                + "    else if(role.includes('buyer') && bStr) { var b = JSON.parse(bStr); if(b.photo) photo = b.photo; else if(p.photo) photo = p.photo; }"
                + "    else { if(p.photo) photo = p.photo; }"
                + "    if(photo) {"
                + "      el.classList.add('has-photo');"
                + "      el.style.setProperty('--avatar-photo', 'url(\"' + photo + '\")');"
                + "      el.textContent = '';"
                + "    } else {"
                + "      el.classList.remove('has-photo');"
                + "      el.style.removeProperty('--avatar-photo');"
                + "    }"
                + "  }"
                + "  fixHomeAvatar();"
                + "  window.addEventListener('storage', fixHomeAvatar);"
                + "  setInterval(fixHomeAvatar, 1000);"
                + "});"
                + "</script>";
    }

    static String categoryTopic(String title, String copy, String href) {
        return "<article class='topic premium-topic'><h3>" + esc(title) + "</h3><p>" + esc(copy) + "</p><a href='"
                + href + "'>Explore</a></article>";
    }

    static String topicCard(String title, String copy) {
        return "<article class='topic premium-topic'><h3>" + esc(title) + "</h3><p>" + esc(copy) + "</p></article>";
    }

    static String reviewSnippet(String title, String copy) {
        return "<article class='topic premium-topic review-tile'><div class='mini-stars'>★★★★★</div><h3>" + esc(title)
                + "</h3><p>" + esc(copy) + "</p></article>";
    }

    static String roleDashboard(AppUser user) {
        if (user == null)
            return "";
        if (user.isAdmin()) {
            return "<section class='container section role-dashboard'><div class='section-head'><div><p class='eyebrow'>ADMIN CONTROL PANEL</p><h2>Modern moderation and platform overview</h2></div><a href='/admin'>Open Admin Panel</a></div><div class='topic-grid'>"
                    + dashboardTile("Total Users", "Manage buyers, sellers, and staff accounts")
                    + dashboardTile("Pending Approvals", "Review listings and seller verification status")
                    + dashboardTile("Reports & Complaints", "Catch fake listings and moderation issues early")
                    + dashboardTile("Traffic Analytics", "Track marketplace activity and listing performance")
                    + "</div></section>";
        }
        if ("seller".equalsIgnoreCase(user.getRole())) {
            return "<section class='container section role-dashboard'><div class='section-head'><div><p class='eyebrow'>SELLER WORKSPACE</p><h2>Premium sales tools for your active listings</h2></div><a href='/settings'>Open Seller Dashboard</a></div><div class='topic-grid'>"
                    + dashboardTile("My Listings", "Add, edit, promote, and mark vehicles as sold")
                    + dashboardTile("Pending Requests", "Review offers, inquiries, and buyer inspection requests")
                    + dashboardTile("Performance Analytics", "Watch views, leads, and best-performing vehicles")
                    + dashboardTile("Response Rate", "Keep communication fast and premium")
                    + "</div></section>";
        }
        return "<section class='container section role-dashboard'><div class='section-head'><div><p class='eyebrow'>BUYER DASHBOARD</p><h2>Elegant tools for finding the right vehicle</h2></div><a href='/requests'>Track Requests</a></div><div class='topic-grid'>"
                + dashboardTile("Saved Cars", "Keep your shortlist ready for faster comparison")
                + dashboardTile("Recent Searches", "Resume shopping without starting over")
                + dashboardTile("Request Status", "Follow inspection, approval, and offer flow")
                + dashboardTile("Recommended Vehicles", "Discover premium matches based on your browsing")
                + "</div></section>";
    }

    static String dashboardTile(String title, String copy) {
        return "<article class='topic premium-topic dashboard-tile'><h3>" + esc(title) + "</h3><p>" + esc(copy)
                + "</p></article>";
    }

    static String inventoryContent(Map<String, String> params) throws IOException {
        List<Vehicle> all = loadVehicles();
        String selected = normalizeInventoryCategory(params.getOrDefault("category", "all"));
        String q = safe(params.getOrDefault("q", ""));
        String fuel = normalizeFilterValue(params.getOrDefault("fuel", "all"), fuels());
        String transmission = normalizeFilterValue(params.getOrDefault("transmission", "all"), transmissions());
        String sort = normalizeSort(params.getOrDefault("sort", "latest"));

        List<Vehicle> filtered = new ArrayList<>();
        for (Vehicle v : all) {
            if (!"all".equals(selected) && !v.category.equals(selected)) {
                continue;
            }
            if (!q.isBlank() && !matchesSearch(v, q)) {
                continue;
            }
            if (!"all".equals(fuel) && !v.fuel.equalsIgnoreCase(fuel)) {
                continue;
            }
            if (!"all".equals(transmission) && !v.transmission.equalsIgnoreCase(transmission)) {
                continue;
            }
            filtered.add(v);
        }
        if ("price-low".equals(sort) || "price-high".equals(sort)) {
            VehicleLinkedList linkedList = new VehicleLinkedList();
            for (Vehicle vehicle : filtered)
                linkedList.insert(vehicle);
            filtered = linkedList.mergeSortByPrice("price-high".equals(sort));
        } else {
            sortVehicles(filtered, sort);
        }

        StringBuilder tabs = new StringBuilder(
                "<div class='category-tabs'>" + tab("all", selected, q, fuel, transmission, sort, "All"));
        for (String c : categories()) {
            tabs.append(tab(c, selected, q, fuel, transmission, sort, displayCategory(c)));
        }
        tabs.append("</div>");

        String toolbar = "<form class='inventory-toolbar panel' method='get' action='/inventory'>"
                + "<input type='hidden' name='category' value='" + esc(selected) + "'>"
                + "<label class='search-field'><span>Search inventory</span><input name='q' value='" + esc(q)
                + "' placeholder='Search by model, brand, or keyword'></label>"
                + "<label><span>Fuel</span><select name='fuel'>" + optionsForFilter(fuels(), fuel) + "</select></label>"
                + "<label><span>Transmission</span><select name='transmission'>"
                + optionsForFilter(transmissions(), transmission) + "</select></label>"
                + "<label><span>Sort by</span><select name='sort'>" + sortOptions(sort) + "</select></label>"
                + "<div class='toolbar-actions'><button class='btn btn-primary' type='submit'>Apply</button><button class='btn btn-secondary' type='submit' name='sort' value='price-low'>Sort Button: Price Low</button><a class='btn btn-secondary' href='/inventory'>Reset</a></div>"
                + "</form>";

        StringBuilder cards = new StringBuilder();
        for (Vehicle v : filtered) {
            cards.append(card(v));
        }
        String count = "<p class='inventory-results'>Showing <strong>" + filtered.size() + "</strong> vehicle"
                + (filtered.size() == 1 ? "" : "s") + "</p>";
        if (cards.length() == 0) {
            cards.append(
                    "<div class='panel empty-state'><p>No vehicles match your current search and filters.</p></div>");
        }
        return "<section class='container section'><p class='eyebrow'>DYNAMIC CAR DISPLAY PAGE</p><h1>Browse by category</h1><p class='lead'>Cars are loaded dynamically into a Linked List, and the price sort uses Merge Sort.</p>"
                + tabs + toolbar + count + "<div class='grid cards'>" + cards + "</div></section>";
    }

    static String sellContent(String error) {
        String errorHtml = error == null ? "" : "<div class='alert'>" + esc(error) + "</div>";
        StringBuilder opts = new StringBuilder();
        for (String c : categories()) {
            opts.append("<option value='").append(c).append("'>").append(displayCategory(c)).append("</option>");
        }
        return "<section class='container section'><p class='eyebrow'>SELL YOUR CAR</p><h1>List a vehicle in the correct category</h1><p class='lead'>Each submitted vehicle is stored under its matching category and opens on a dedicated detail page with submission date and time.</p>"
                + errorHtml
                + "<form class='sell-form panel' method='post' action='/sell'>"
                + field("Vehicle title", "title", "Toyota RAV4 Adventure")
                + "<div class='form-grid'>"
                + selectField("Category", "category", opts.toString())
                + field("Price (LKR)", "price", "16500000")
                + field("Year", "year", "2021")
                + field("Mileage", "mileage", "42,000 km")
                + field("Fuel", "fuel", "Hybrid")
                + field("Transmission", "transmission", "Automatic")
                + "</div>"
                + "<label>Vehicle image upload<input id='sellImageFile' type='file' accept='image/*'><input id='sellImageData' type='hidden' name='image'><span class='hint'>Upload from your computer. Preview appears before saving.</span></label><div id='sellImagePreview' class='panel' style='display:none;height:180px;background-size:cover;background-position:center;border-radius:16px;margin:10px 0;'></div>"
                + textareaField("Sales description", "description",
                        "Certified, inspected, and ready to roll with premium comfort, strong resale value, and confidence-building history.")
                + "<button class='btn btn-primary' type='submit'>Submit for Admin Approval</button></form>"
                + "<script>document.addEventListener('DOMContentLoaded',()=>{const f=document.getElementById('sellImageFile'),d=document.getElementById('sellImageData'),p=document.getElementById('sellImagePreview');if(f)f.onchange=()=>{const file=f.files&&f.files[0];if(!file)return;if(!file.type.startsWith('image/')){alert('Choose a valid image');f.value='';return;}const r=new FileReader();r.onload=()=>{d.value=r.result;p.style.display='block';p.style.backgroundImage='url(\"'+r.result+'\")'};r.readAsDataURL(file);};document.querySelector('form[action=\\'/sell\\']')?.addEventListener('submit',()=>{try{const list=JSON.parse(localStorage.getItem('automartLiveNotificationsV1')||'[]');list.unshift({id:'seller_'+Date.now(),title:'Listing submitted',body:'Your vehicle listing was sent for admin approval.',time:'Now',read:false});localStorage.setItem('automartLiveNotificationsV1',JSON.stringify(list));}catch(e){}});});</script></section>";
    }

    static String detailContent(Vehicle v) {
        String savedMessage = "<script>if(new URLSearchParams(location.search).get('saved')==='1'){document.write(\"<div class='container'><div class='alert success'>New car details saved successfully.</div></div>\");}</script>";
        String management = "";
        if (v.uploaded) {
            management = "<div class='detail-management panel reveal-up'><div><p class='eyebrow'>VEHICLE MANAGEMENT</p><h3>Edit or remove this uploaded vehicle</h3><p>Update listing details from your work settings area or remove the vehicle from recent uploads.</p></div><div class='actions'><a class='btn btn-primary' href='/vehicle/edit?id="
                    + url(v.id)
                    + "'>Edit Vehicle</a><form method='post' action='/vehicle/delete' data-delete-confirm=\"Delete this vehicle from Auto Mart?\"><input type='hidden' name='id' value='"
                    + esc(v.id)
                    + "'><button class='btn btn-danger' type='submit'>Delete Vehicle</button></form></div></div>";
        }
        return savedMessage + "<section class='container section detail'>"
                + "<div class='detail-grid'>"
                + "<img class='detail-image detail-visual' src='" + imageSrc(v.image) + "' alt='" + esc(v.title) + "'>"
                + "<div><p class='eyebrow'>" + esc(displayCategory(v.category)) + "</p><h1>" + esc(v.title)
                + "</h1><p class='price'>" + esc(formatLkr(v.price)) + "</p>"
                + "<div class='detail-description panel reveal-detail'><h3>Full description</h3><div class='detail-description-copy'>"
                + formatDescriptionHtml(v.description) + "</div></div>"
                + "<div class='spec-grid'>" + spec("Year", v.year) + spec("Mileage", v.mileage) + spec("Fuel", v.fuel)
                + spec("Transmission", v.transmission) + spec("Listed", formatTime(v.createdAt))
                + spec("Saved To", "uploads/vehicle-submissions/" + v.category + "/") + "</div>"
                + management
                + "<div class='detail-premium-actions panel reveal-up'><div class='detail-premium-copy'><span class='eyebrow'>PREMIUM SHORTLIST</span><h3>Save this car and come back to it anytime</h3><p>Use your wishlist as a realistic second-hand buying flow instead of a normal shopping cart.</p></div><div class='actions'><button class='btn btn-primary wishlist-btn detail-wishlist-btn' type='button' "
                + wishlistAttrs(v)
                + ">Add to Wishlist</button><a class='btn btn-secondary' href='/wishlist'>Open Wishlist</a></div></div>"
                + "<div class='actions'><a class='btn btn-primary' href='/inventory?category=" + esc(v.category)
                + "'>More " + esc(displayCategory(v.category))
                + "</a><a class='btn btn-secondary' href='/uploads'>Recent Uploads</a><a class='btn btn-secondary' href='/settings'>Work Settings</a></div>"
                + "</div></div></section>";
    }

    static void updateSubmission(Vehicle existing, Map<String, String> form) throws IOException {
        String category = normalizeCategory(form.getOrDefault("category", existing.category));
        Vehicle updated = new Vehicle(
                existing.id,
                category,
                safe(form.getOrDefault("title", existing.title)),
                normalizePriceLkr(form.getOrDefault("price", existing.price)),
                safe(form.getOrDefault("year", existing.year)),
                safe(form.getOrDefault("mileage", existing.mileage)),
                safe(form.getOrDefault("fuel", existing.fuel)),
                safe(form.getOrDefault("transmission", existing.transmission)),
                safe(form.getOrDefault("description", existing.description)),
                normalizeSubmittedImage(category, safe(form.getOrDefault("image", existing.image))),
                existing.createdAt,
                true);
        List<Vehicle> vehicles = loadVehicles();
        for (int i = 0; i < vehicles.size(); i++) {
            if (vehicles.get(i).id.equals(existing.id)) {
                vehicles.set(i, updated);
                break;
            }
        }
        FileHelper.writeVehicles(vehicles);
        if (!existing.category.equals(updated.category)) {
            Files.deleteIfExists(UPLOAD_ROOT.resolve(existing.category).resolve(existing.id + ".txt"));
        }
        Path categoryDir = UPLOAD_ROOT.resolve(updated.category);
        Files.createDirectories(categoryDir);
        Files.writeString(categoryDir.resolve(updated.id + ".txt"), updated.toText(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    static void deleteSubmission(String id) throws IOException {
        List<Vehicle> vehicles = loadVehicles();
        Vehicle removed = null;
        List<Vehicle> kept = new ArrayList<>();
        for (Vehicle v : vehicles) {
            if (v.id.equals(id) && v.uploaded) {
                removed = v;
                continue;
            }
            kept.add(v);
        }
        FileHelper.writeVehicles(kept);
        if (removed != null) {
            Files.deleteIfExists(UPLOAD_ROOT.resolve(removed.category).resolve(removed.id + ".txt"));
            logActivity("LISTING_DELETED", "Deleted listing " + removed.title + " (" + removed.id + ")");
        }
    }

    static void rewriteSubmissionFile(List<Vehicle> vehicles) throws IOException {
        FileHelper.writeVehicles(vehicles);
    }

    static String settingsContent(HttpExchange exchange, String message) throws IOException {
        Settings settings = readSettings(exchange);
        List<Vehicle> vehicles = loadVehicles();
        StringBuilder rows = new StringBuilder();
        int activeListings = 0;
        int uploadedCount = 0;
        int soldHistory = 0;
        int listingLimit = 5;
        int listingShown = 0;
        for (Vehicle v : vehicles) {
            if (v.uploaded) {
                uploadedCount++;
                activeListings++;
            } else if (soldHistory < 4) {
                soldHistory++;
            }
            if (listingShown >= listingLimit) {
                continue;
            }
            listingShown++;
            rows.append("<tr><td><a href='/vehicle?id=").append(url(v.id)).append("'>").append(esc(v.title))
                    .append("</a></td><td>")
                    .append(esc(displayCategory(v.category))).append("</td><td>")
                    .append(esc(formatLkr(v.price))).append("</td><td>")
                    .append(esc(formatTime(v.createdAt))).append("</td><td><div class='manage-actions'>");
            if (v.uploaded) {
                rows.append("<a class='manage-link' href='/vehicle/edit?id=").append(url(v.id)).append("'>Edit</a>")
                        .append("<a class='manage-link' href='/vehicle?id=").append(url(v.id)).append("'>Preview</a>")
                        .append("<form method='post' action='/vehicle/delete' data-delete-confirm=\"Delete this uploaded vehicle?\"><input type='hidden' name='id' value='")
                        .append(esc(v.id))
                        .append("'><button class='manage-delete' type='submit'>Delete</button></form>");
            } else {
                rows.append("<a class='manage-link' href='/vehicle?id=").append(url(v.id))
                        .append("'>Preview</a><span class='table-note'>Seeded showroom vehicle</span>");
            }
            rows.append("</div></td></tr>");
        }
        if (rows.length() == 0) {
            rows.append("<tr><td colspan='5' class='uploads-empty'>No listings available yet.</td></tr>");
        }
        String listingSummary = "Showing the latest " + Math.min(listingLimit, vehicles.size()) + " of "
                + vehicles.size() + " listings here.";
        String listingActions = "<div class='settings-action-row compact-row'><a class='btn btn-secondary' href='/uploads'>Recent Uploads</a><a class='btn btn-secondary' href='/inventory'>Browse Inventory</a><a class='btn btn-primary' href='/sell'>Add Vehicle</a></div>";
        String msg = message == null ? "" : "<div class='alert success reveal-up'>" + esc(message) + "</div>";

        return "<section class='container section settings-workbench settings-premium-v2'>"
                + msg
                + "<div class='settings-shell settings-shell-premium'>"
                + "<aside class='panel settings-sidebar settings-sidebar-premium reveal-up'>"
                + "<div class='settings-sidebar-head'><p class='eyebrow'>SETTINGS</p><h2>Dealer Control Center</h2><p class='settings-sidebar-copy'>Professional used-car marketplace settings with billing, listings, trust controls, social sign-in options, and better scrolling behavior.</p></div>"
                + "<div class='settings-profile-chip'><div class='settings-avatar'>" + esc(initials(settings.fullName))
                + "</div><div><strong>" + esc(settings.fullName) + "</strong><span>@"
                + esc(readCookie(exchange, AUTH_COOKIE)) + "</span></div></div>"
                + "<nav class='settings-nav settings-nav-premium'>"
                + settingsNavLink("#profile", navIcon("dashboard"), "Business")
                + settingsNavLink("#alerts", navIcon("requests"), "Leads")
                + settingsNavLink("#verification", navIcon("security"), "Trust")
                + settingsNavLink("#garage", navIcon("inventory"), "Listings")
                + settingsNavLink("#seller", navIcon("sell"), "Delivery")
                + settingsNavLink("#billing", navIcon("settings"), "Billing")
                + settingsNavLink("#access", navIcon("dashboard"), "Access")
                + settingsNavLink("#security", navIcon("security"), "Security")
                + "</nav>"
                + "<div class='sidebar-stats'>"
                + infoTile("Plan", settings.subscriptionPlan)
                + infoTile("Active listings", String.valueOf(activeListings))
                + infoTile("Credits", settings.featuredCredits)
                + "</div>"
                + "</aside>"
                + "<div class='settings-main settings-main-premium'>"
                + "<section class='panel settings-overview-card reveal-up'>"
                + "<div class='settings-overview-copy'><p class='eyebrow'>CONTROL CENTER</p><h1>Professional dealer settings workspace</h1><p class='lead'>Built for a second-hand car marketplace with attractive motion, better spacing, fixed sidebar behavior, cleaner toggles, and more powerful sales settings.</p></div>"
                + "<div class='settings-overview-stats'><article><span>Profile completion</span><strong>92%</strong><small>Looks trusted and buyer-ready</small></article><article><span>Security health</span><strong>96%</strong><small>Strong account protection</small></article><article><span>Saved alerts</span><strong>3</strong><small>Monitoring matches live</small></article></div>"
                + "</section>"
                + "<form class='settings-form-stack settings-form' method='post' action='/settings'>"

                + settingsSectionOpen("profile", "Profile &amp; Personalization",
                        "Update identity, contact details, preferences, and the main account presentation.")
                + "<div class='settings-form-grid premium-form-grid'>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Main identity</h3><p>Your public-facing account information.</p></div></div><div class='settings-field-grid two-col'>"
                + fieldWithValue("Full name", "fullName", "Auto Mart Manager", settings.fullName)
                + fieldWithValue("Email address", "teamEmail", "team.automart@gmail.com", settings.teamEmail)
                + fieldWithValue("Phone number", "phone", "+94 77 123 4567", settings.phone)
                + fieldWithValue("Location", "location", "Colombo, Western Province", settings.location)
                + fieldWithValue("Profile photo URL", "profilePhoto", "https://...", settings.profilePhoto)
                + fieldWithValue("Premium badge status", "premiumBadgeStatus", "Verified seller",
                        settings.premiumBadgeStatus)
                + fieldWithValue("Dealer / showroom name", "dealerName", "Auto Mart Premium Dealer",
                        settings.dealerName)
                + fieldWithValue("WhatsApp number", "whatsappNumber", "+94 77 222 3344", settings.whatsappNumber)
                + fieldWithValue("Business registration no", "registrationNumber", "PV 123456",
                        settings.registrationNumber)
                + textareaFieldWithValue("Business address", "businessAddress", "No. 45, Colombo 05",
                        settings.businessAddress)
                + "</div></div>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Workspace preferences</h3><p>Choose how the workspace behaves by default.</p></div></div><div class='settings-field-grid two-col'>"
                + selectField("Theme", "theme",
                        selectOptions(new String[][] { { "Dark", "Dark" }, { "Light", "Light" },
                                { "System", "System Default" } }, settings.theme))
                + selectField("Default dashboard", "defaultDashboard",
                        selectOptions(new String[][] { { "inventory", "Inventory" }, { "uploads", "Recent Uploads" },
                                { "settings", "Settings" } }, settings.defaultDashboard))
                + selectField("Workspace mode", "workspaceMode",
                        selectOptions(new String[][] { { "operations", "Operations" }, { "sales", "Sales" },
                                { "executive", "Executive" } }, settings.workspaceMode))
                + fieldWithValue("Default city / zip", "defaultCity", "Colombo 03", settings.defaultCity)
                + fieldWithValue("Preferred local feed", "preferredLocationFirst", "Colombo",
                        settings.preferredLocationFirst)
                + textareaFieldWithValue("Short bio", "bio",
                        "Trusted marketplace operator focused on premium listings and fast communication.",
                        settings.bio)
                + selectField("Seller type", "sellerType",
                        selectOptions(new String[][] { { "dealer", "Dealer" }, { "showroom", "Showroom" },
                                { "individual", "Individual" } }, settings.sellerType))
                + fieldWithValue("Google Maps URL", "mapsUrl", "https://maps.google.com/...", settings.mapsUrl)
                + "</div><div class='toggle-grid three-col compact-toggle-grid'>"
                + toggle("Verified seller badge", "verifiedSeller", settings.verifiedSeller)
                + toggle("SMS alerts", "smsAlerts", settings.smsAlerts)
                + toggle("Push alerts", "pushAlerts", settings.pushAlerts)
                + "</div></div>"
                + "</div>"
                + settingsSectionClose()

                + settingsSectionOpen("alerts", "Alerts &amp; Notifications",
                        "Control saved searches, price drops, contact channels, and how often updates arrive.")
                + "<div class='settings-form-grid premium-form-grid'>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Notification toggles</h3><p>Turn on the channels that matter for you.</p></div></div><div class='toggle-grid three-col compact-toggle-grid'>"
                + toggle("Price drop alerts", "priceDropAlerts", settings.priceDropAlerts)
                + toggle("Saved search alerts", "savedSearchAlerts", settings.savedSearchAlerts)
                + toggle("Chat notifications", "chatNotifications", settings.chatNotifications)
                + toggle("Email notifications", "emailNotifications", settings.emailNotifications)
                + toggle("Concierge mode", "conciergeMode", settings.conciergeMode)
                + toggle("Inventory sync notices", "inventorySync", settings.inventorySync)
                + "</div></div>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Alert delivery</h3><p>Set the delivery style and preferred contact route.</p></div></div><div class='settings-field-grid two-col'>"
                + selectField("Notification frequency", "notificationFrequency",
                        selectOptions(new String[][] { { "Immediate", "Immediate" }, { "Daily Digest", "Daily Digest" },
                                { "None", "None" } }, settings.notificationFrequency))
                + selectField("Preferred contact method", "preferredContact",
                        selectOptions(new String[][] { { "WhatsApp", "WhatsApp" }, { "Phone", "Phone" },
                                { "In-app Chat", "In-app Chat" } }, settings.preferredContact))
                + fieldWithValue("Alert focus label", "alertFocusLabel", "Premium buyers first",
                        settings.workspaceMode.equals("sales") ? "Sales pipeline" : "Premium buyers first")
                + fieldWithValue("Saved search spotlight", "savedSearchLabel", "SUV under LKR 20M",
                        "Luxury SUV under LKR 20M")
                + "</div><div class='settings-card-grid two-card-grid'>"
                + settingsMiniCard("Search Alert", "Land Rover Defender 110 under LKR 50M", "Edit", "Delete",
                        "search-alert")
                + settingsMiniCard("Search Alert", "BMW hybrid sedan under 40,000 km", "Edit", "Delete", "search-alert")
                + "</div></div>"
                + "</div>"
                + settingsSectionClose()

                + settingsSectionOpen("verification", "Trust, Verification &amp; Buyer Confidence",
                        "Show buyers the signals that matter: inspection, warranty, finance, and genuine seller trust badges.")
                + "<div class='settings-form-grid premium-form-grid'>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Verification profile</h3><p>Buyer-facing trust markers for second-hand car listings.</p></div></div><div class='toggle-grid three-col compact-toggle-grid'>"
                + toggle("Verified seller badge", "verifiedSeller", settings.verifiedSeller)
                + toggle("Inspection available", "inspectionAvailable", settings.inspectionAvailable)
                + toggle("Service history shown", "serviceHistoryEnabled", settings.serviceHistoryEnabled)
                + toggle("VIN / chassis field", "vinEnabled", settings.vinEnabled)
                + toggle("Finance available", "financeAvailable", settings.financeAvailable)
                + toggle("Trade-in accepted", "tradeInEnabled", settings.tradeInEnabled)
                + toggle("Warranty offered", "warrantyEnabled", settings.warrantyEnabled)
                + toggle("Document verification", "documentVerification", settings.documentVerification)
                + toggle("Accident history field", "accidentHistoryEnabled", settings.accidentHistoryEnabled)
                + "</div></div>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Trust content</h3><p>Messages shown to buyers on listing pages and public profile cards.</p></div></div><div class='settings-field-grid two-col'>"
                + fieldWithValue("Warranty note", "warrantyNote", "3-month engine & gearbox warranty",
                        settings.warrantyNote)
                + fieldWithValue("Finance partner", "financePartner", "BOC / Commercial Bank", settings.financePartner)
                + fieldWithValue("Trade-in policy", "tradeInPolicy", "Trade-ins accepted after valuation",
                        settings.tradeInPolicy)
                + fieldWithValue("Response SLA", "responseSla", "Within 15 minutes", settings.responseSla)
                + "</div></div>"
                + "</div>"
                + settingsSectionClose()

                + settingsSectionOpen("security", "Security &amp; Account Safety",
                        "Keep account access premium, clear, and safe without cramped controls.")
                + "<div class='settings-security-layout'>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Password security</h3><p>Change the password and keep the account protected.</p></div><span class='split-chip'>Encrypted</span></div><form class='premium-password-form simple-password-form' method='post' action='/settings/action'>"
                + hiddenField("action", "password")
                + "<div class='settings-field-grid one-col'>"
                + "<label><span>Current password</span><input type='password' name='currentPassword' placeholder='Current password' required></label>"
                + "<label><span>New password</span><input type='password' name='newPassword' placeholder='New password' required></label>"
                + "<label><span>Confirm new password</span><input type='password' name='confirmPassword' placeholder='Confirm new password' required></label>"
                + "</div><div class='settings-action-row'><button class='btn btn-primary security-submit-btn' data-action-name='Change Password' type='submit'><span class='security-btn-label'>Change Password</span><span class='security-btn-loader' aria-hidden='true'></span></button><button class='btn btn-secondary' type='button' data-toggle-password>Show / Hide</button></div></form></div>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Protection controls</h3><p>Core safety tools and visibility controls.</p></div></div><div class='toggle-grid three-col compact-toggle-grid security-toggle-grid'>"
                + toggle("Two-factor auth", "twoFactorAuth", settings.twoFactorAuth)
                + toggle("Privacy mode", "privacyMode", settings.privacyMode)
                + toggle("Session shield", "sessionShield", settings.sessionShield)
                + toggle("Show phone number", "showPhoneNumber", settings.showPhoneNumber)
                + toggle("Export ready", "dataExportReady", settings.dataExportReady)
                + toggle("Verified seller", "verifiedSeller", settings.verifiedSeller)
                + "</div><div class='settings-card-grid two-card-grid compact-health-grid'>"
                + settingsMiniCard("Security", "Protection score: 96%", "Review", "Later", "score")
                + settingsMiniCard("Trusted device", "Colombo workspace active now", "Manage", "Sign out", "device")
                + "</div></div>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Blocked users</h3><p>Keep unwanted users out of the conversation flow.</p></div></div><label><span>Blocked usernames</span><textarea name='blockedUsers' rows='4' placeholder='lowballer_01, spam-buyer-22'>"
                + esc(settings.blockedUsers)
                + "</textarea></label><p class='inline-copy'>Separate usernames with commas. These values save with the rest of your settings.</p></div>"
                + "<div class='settings-card-grid two-card-grid'>"
                + securityBottomCard("Download Data", "Export profile, garage, and preferences as a secure file.",
                        "download-data", "Download Data", false, "JSON + TXT export")
                + securityBottomCard("Delete Account", "Clear saved settings and sign out after confirmation.",
                        "delete-account", "Delete Account", true, "This action cannot be undone.")
                + "</div>"
                + "</div>"
                + settingsSectionClose()

                + settingsSectionOpen("garage", "Garage &amp; Buyer Preferences",
                        "Tune units, currency, wishlist behavior, and watchlist style.")
                + "<div class='settings-form-grid premium-form-grid'>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Garage preferences</h3><p>Presentation, units, and local browsing defaults.</p></div></div><div class='settings-field-grid two-col'>"
                + selectField("Distance units", "distanceUnit",
                        selectOptions(new String[][] { { "Kilometers", "Kilometers" }, { "Miles", "Miles" } },
                                settings.distanceUnit))
                + selectField("Currency", "currency",
                        selectOptions(new String[][] { { "LKR", "LKR" }, { "USD", "USD" }, { "EUR", "EUR" } },
                                settings.currency))
                + fieldWithValue("Preferred location first", "preferredLocationFirst", "Colombo",
                        settings.preferredLocationFirst)
                + fieldWithValue("Wishlist spotlight", "wishlistSpotlight", "Premium German sedans",
                        settings.wishlistSpotlight)
                + selectField("Default price type", "priceType",
                        selectOptions(new String[][] { { "Fixed", "Fixed" }, { "Negotiable", "Negotiable" },
                                { "Call for price", "Call for price" } }, settings.priceType))
                + selectField("Listing duration", "listingDuration",
                        selectOptions(new String[][] { { "14 Days", "14 Days" }, { "30 Days", "30 Days" },
                                { "60 Days", "60 Days" } }, settings.listingDuration))
                + fieldWithValue("Minimum photos", "minPhotos", "8", settings.minPhotos)
                + "</div><div class='toggle-grid three-col compact-toggle-grid'>"
                + toggle("Islandwide delivery", "islandwideDelivery", settings.islandwideDelivery)
                + toggle("Home test drive", "homeTestDrive", settings.homeTestDrive)
                + toggle("Show previous owners", "previousOwnersEnabled", settings.previousOwnersEnabled)
                + "</div></div>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Watchlist &amp; favorites</h3><p>Quick control cards for saved vehicles.</p></div></div><div class='settings-card-grid two-card-grid'>"
                + settingsMiniCard("Watchlist", "BMW i4 M50 • Price drop tracked", "Rename", "Remove", "watchlist")
                + settingsMiniCard("Watchlist", "Ram 1500 Rebel • Seller responded", "Rename", "Remove", "watchlist")
                + settingsMiniCard("Watchlist", "MINI Countryman • Availability alerts on", "Rename", "Remove",
                        "watchlist")
                + settingsMiniCard("Wishlist", "Audi performance sedans • High priority", "Edit", "Remove", "wishlist")
                + "</div></div>"
                + "</div>"
                + settingsSectionClose()

                + settingsSectionOpen("seller", "Seller Tools &amp; Listings",
                        "Business defaults, automation, and quick listing management.")
                + "<div class='settings-form-grid premium-form-grid'>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Seller automation</h3><p>Set up repeat behaviors for listings and lead handling.</p></div></div><div class='toggle-grid three-col compact-toggle-grid'>"
                + toggle("Auto-renew listings", "autoRenew", settings.autoRenew)
                + toggle("Hide phone number", "hidePhone", settings.hidePhone)
                + toggle("Automated replies", "automatedReplies", settings.automatedReplies)
                + toggle("Google Vehicle Ads sync", "googleVehicleAds", settings.googleVehicleAds)
                + toggle("DMS inventory sync", "dmsSync", settings.dmsSync)
                + toggle("Featured ad boost", "featuredBoost", settings.featuredBoost)
                + "</div><div class='settings-field-grid two-col'>"
                + fieldWithValue("Default pickup location", "defaultPickup", "Rajagiriya vehicle yard",
                        settings.defaultPickup)
                + fieldWithValue("Business hours", "businessHours", "Mon-Sat • 9:00 AM - 6:00 PM",
                        settings.businessHours)
                + fieldWithValue("Away / automated reply", "awayMessage",
                        "Thanks for your message. We will reply shortly.", settings.awayMessage)
                + fieldWithValue("Featured credits", "featuredCredits", "12", settings.featuredCredits)
                + fieldWithValue("Delivery fee", "deliveryFee", "LKR 12,500", settings.deliveryFee)
                + fieldWithValue("Test drive slot length", "testDriveSlot", "45 minutes", settings.testDriveSlot)
                + "</div></div>"
                + "<div class='panel vehicle-management-panel nested-panel'><div class='section-head'><div><p class='eyebrow'>MY LISTINGS</p><h2>Recent uploaded vehicles</h2><p class='section-copy'>"
                + listingSummary
                + "</p></div></div><table class='uploads-table vehicle-management-table'><thead><tr><th>Vehicle</th><th>Category</th><th>Price</th><th>Listed</th><th>Actions</th></tr></thead><tbody>"
                + rows + "</tbody></table>" + listingActions + "</div>"
                + "</div>"
                + settingsSectionClose()

                + settingsSectionOpen("access", "Access, Sign In &amp; Social Login",
                        "Control which sign-in methods are visible across your website.")
                + "<div class='settings-form-grid premium-form-grid'>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Allowed sign-in methods</h3><p>Enable fast onboarding channels for buyers and sellers.</p></div></div><div class='toggle-grid three-col compact-toggle-grid'>"
                + toggle("Email / password login", "allowEmailLogin", settings.allowEmailLogin)
                + toggle("Google sign in", "socialGoogle", settings.socialGoogle)
                + toggle("Facebook sign in", "socialFacebook", settings.socialFacebook)
                + toggle("LinkedIn sign in", "socialLinkedin", settings.socialLinkedin)
                + toggle("Social signup shortcuts", "socialSignup", settings.socialSignup)
                + toggle("Animated auth transitions", "animatedAuth", settings.animatedAuth)
                + "</div></div>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Onboarding preferences</h3><p>Shape the sign-up experience for your marketplace.</p></div></div><div class='settings-field-grid two-col'>"
                + selectField("Default signup role", "defaultSignupRole",
                        selectOptions(new String[][] { { "seller", "Seller" }, { "buyer", "Buyer" } },
                                settings.defaultSignupRole))
                + fieldWithValue("Welcome headline", "welcomeHeadline", "Sell smarter on Auto Mart",
                        settings.welcomeHeadline)
                + fieldWithValue("Welcome subtext", "welcomeSubtext",
                        "Trusted used-car buying and selling in Sri Lanka", settings.welcomeSubtext)
                + fieldWithValue("Payment method default", "paymentMethod", "Bank Transfer", settings.paymentMethod)
                + "</div></div>"
                + "</div>"
                + settingsSectionClose()

                + settingsSectionOpen("billing", "Billing &amp; Plan",
                        "Subscription, invoice email, and premium feature credit controls.")
                + "<div class='settings-form-grid premium-form-grid'>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Plan details</h3><p>Manage subscriptions and invoice destination.</p></div></div><div class='settings-field-grid two-col'>"
                + selectField("Subscription plan", "subscriptionPlan",
                        selectOptions(new String[][] { { "Starter", "Starter" }, { "Premium Dealer", "Premium Dealer" },
                                { "Enterprise", "Enterprise" } }, settings.subscriptionPlan))
                + fieldWithValue("Billing history / invoice email", "billingEmail", "billing.automart@gmail.com",
                        settings.billingEmail)
                + fieldWithValue("Featured ad credits", "featuredCredits", "12", settings.featuredCredits)
                + fieldWithValue("Plan note", "planNote", "Premium seller with boosted listing tools",
                        settings.planNote)
                + selectField("Payment method", "paymentMethod", selectOptions(
                        new String[][] { { "Bank Transfer", "Bank Transfer" }, { "Card Payment", "Card Payment" },
                                { "Cash Collection", "Cash Collection" }, { "Mobile Wallet", "Mobile Wallet" } },
                        settings.paymentMethod))
                + fieldWithValue("Bank name", "bankName", "Bank of Ceylon", settings.bankName)
                + fieldWithValue("Account holder", "accountName", "Auto Mart Pvt Ltd", settings.accountName)
                + fieldWithValue("Account number", "accountNumber", "1234567890", settings.accountNumber)
                + "</div></div>"
                + "<div class='panel settings-subcard'><div class='subpanel-head'><div><h3>Billing quick actions</h3><p>Shortcuts for premium monetization controls.</p></div></div><div class='settings-card-grid two-card-grid'>"
                + settingsMiniCard("Credits", "Boost wallet balance: " + settings.featuredCredits + " credits",
                        "Top up", "Reset", "credits")
                + settingsMiniCard("Invoices", "Billing updates sent to " + settings.billingEmail, "View", "Change",
                        "invoices")
                + settingsMiniCard("Subscription", "Current plan: " + settings.subscriptionPlan, "Upgrade", "Downgrade",
                        "subscription")
                + settingsMiniCard("Revenue", "Uploaded listings: " + uploadedCount + " active", "Review", "Hide",
                        "revenue")
                + "</div></div>"
                + "</div>"
                + "<div class='settings-action-row settings-save-row'><button class='btn btn-primary pulse-btn' type='submit'>Save All Settings</button><a class='btn btn-secondary' href='/settings/export'>Export Settings</a><a class='btn btn-secondary' href='/inventory'>Open Inventory</a></div>"
                + settingsSectionClose()
                + "</form>"
                + "</div></div></section>";
    }

    static String settingsUserManagementRows() {
        StringBuilder rows = new StringBuilder();
        try {
            for (AppUser user : loadUsers()) {
                rows.append("<tr><td><strong>").append(esc(user.getUsername())).append("</strong></td><td>")
                        .append(esc(user.getRole()))
                        .append("</td><td><span class='table-status is-live'>Active</span></td><td>")
                        .append(esc(user.getEmail())).append("</td></tr>");
            }
        } catch (IOException ignored) {
        }
        if (rows.length() == 0) {
            rows.append("<tr><td colspan='4' class='uploads-empty'>No users found.</td></tr>");
        }
        return rows.toString();
    }

    static String premiumSessionItem(String title, String meta, String age) {
        return "<div class='premium-session-item'><div><strong>" + esc(title) + "</strong><p>" + esc(meta)
                + "</p></div><span>" + esc(age) + "</span></div>";
    }

    static String premiumHealthItem(String label, String value) {
        return "<div class='premium-health-item'><span>" + esc(label) + "</span><strong>" + esc(value)
                + "</strong></div>";
    }

    static String settingsMessage(Map<String, String> query) {
        if (query.containsKey("saved"))
            return "Settings saved successfully.";
        if (query.containsKey("deleted"))
            return "Vehicle deleted successfully.";
        if (query.containsKey("password-updated"))
            return "Password changed successfully.";
        if (query.containsKey("devices-logged-out"))
            return "Other devices were logged out successfully.";
        if (query.containsKey("download-started"))
            return "Your data export is ready.";
        if (query.containsKey("deleted-account"))
            return "Your account settings were cleared.";
        if (query.containsKey("error"))
            return query.get("error");
        return null;
    }

    static String securityStatusMetric(String label, String status, int progress) {
        return "<article class='security-status-metric animated-card'><div class='security-metric-ring' style='--progress:"
                + Math.max(0, Math.min(100, progress)) + "'><span>" + Math.max(0, Math.min(100, progress))
                + "%</span></div><div><strong>" + esc(label) + "</strong><p>" + esc(status) + "</p></div></article>";
    }

    static String securityToggleRow(String icon, String title, String description, String name, boolean checked) {
        return "<label class='security-toggle-row spotlight-card'><span class='security-toggle-meta'><span class='security-toggle-icon'>"
                + icon + "</span><span><strong>" + esc(title) + "</strong><small>" + esc(description)
                + "</small></span></span><span class='security-liquid-toggle'><input type='checkbox' name='" + esc(name)
                + "'" + (checked ? " checked" : "") + "><span class='setting-toggle-ui'></span></span></label>";
    }

    static String securitySessionItem(String title, String meta, String badge) {
        return "<div class='security-session-item'><div><strong>" + esc(title) + "</strong><small>" + esc(meta)
                + "</small></div><span>" + esc(badge) + "</span></div>";
    }

    static String securityBottomCard(String title, String copy, String action, String buttonLabel, boolean danger,
            String meta) {
        String icon = switch (action) {
            case "download-data" -> "📥";
            case "logout-devices" -> "📲";
            case "delete-account" -> "🗑️";
            default -> "⚙️";
        };
        String extraClass = danger ? " danger-zone-card" : "";
        String body = danger
                ? "<p class='inline-copy danger-copy'>" + esc(meta)
                        + "</p><label class='confirm-inline'><input type='checkbox' name='confirmDelete' required><span>I understand this clears saved settings and signs me out.</span></label>"
                : "<p class='inline-copy'>" + esc(meta) + "</p>";
        return "<form class='security-bottom-card spotlight-card animated-card" + extraClass
                + "' method='post' action='/settings/action'>"
                + hiddenField("action", action)
                + "<div class='security-bottom-head'><span class='security-bottom-icon'>" + icon + "</span><div><h4>"
                + esc(title) + "</h4><p>" + esc(copy) + "</p></div></div>"
                + body
                + "<button class='btn security-submit-btn " + (danger ? "btn-danger" : "btn-secondary")
                + "' data-action-name='" + esc(buttonLabel) + "' type='submit'><span class='security-btn-icon'>" + icon
                + "</span><span class='security-btn-label'>" + esc(buttonLabel)
                + "</span><span class='security-btn-loader' aria-hidden='true'></span></button></form>";
    }

    static void handleSettingsAction(HttpExchange exchange, Map<String, String> form) throws IOException {
        String action = safe(form.getOrDefault("action", ""));
        switch (action) {
            case "password" -> {
                String current = safe(form.getOrDefault("currentPassword", ""));
                String next = safe(form.getOrDefault("newPassword", ""));
                String confirm = safe(form.getOrDefault("confirmPassword", ""));
                if (current.isBlank() || next.isBlank() || confirm.isBlank()) {
                    redirect(exchange, "/settings?error=" + url("Fill in all password fields."));
                    return;
                }
                if (!next.equals(confirm)) {
                    redirect(exchange, "/settings?error=" + url("New password and confirmation do not match."));
                    return;
                }
                if (next.length() < 6) {
                    redirect(exchange, "/settings?error=" + url("Use at least 6 characters for the new password."));
                    return;
                }
                redirect(exchange, "/settings?password-updated=1");
            }
            case "logout-devices" -> {
                exchange.getResponseHeaders().add("Set-Cookie", SETTINGS_COOKIE + "=; Path=/; Max-Age=0; SameSite=Lax");
                clearSession(exchange);
                redirect(exchange, "/auth?signedOutAll=1");
            }
            case "download-data" -> redirect(exchange, "/settings/export?download=1");
            case "delete-account" -> {
                AppUser current = currentUser(exchange);
                if (current == null) {
                    redirect(exchange, "/auth?error=" + url("You must be signed in before deleting your account."));
                    return;
                }
                deleteCurrentUser(exchange);
                exchange.getResponseHeaders().add("Set-Cookie", SETTINGS_COOKIE + "=; Path=/; Max-Age=0; SameSite=Lax");
                clearSession(exchange);
                redirect(exchange, "/signup?deleted-account=1");
            }
            default -> redirect(exchange, "/settings?error=" + url("Unsupported settings action."));
        }
    }

    static void sendSettingsExport(HttpExchange exchange) throws IOException {
        Settings s = readSettings(exchange);
        String payload = "Auto Mart Settings Export\n"
                + "Full name: " + s.fullName + "\n"
                + "Dealer name: " + s.dealerName + "\n"
                + "Payment method: " + s.paymentMethod + "\n"
                + "Social login: Google=" + s.socialGoogle + ", Facebook=" + s.socialFacebook + ", LinkedIn="
                + s.socialLinkedin + "\n"
                + "Email: " + s.teamEmail + "\n"
                + "Phone: " + s.phone + "\n"
                + "Location: " + s.location + "\n"
                + "Theme: " + s.theme + "\n"
                + "Default dashboard: " + s.defaultDashboard + "\n"
                + "Notification frequency: " + s.notificationFrequency + "\n"
                + "Preferred contact: " + s.preferredContact + "\n"
                + "Distance unit: " + s.distanceUnit + "\n"
                + "Currency: " + s.currency + "\n"
                + "Subscription plan: " + s.subscriptionPlan + "\n"
                + "Featured credits: " + s.featuredCredits + "\n"
                + "Billing email: " + s.billingEmail + "\n"
                + "Blocked users: " + s.blockedUsers + "\n";
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=automart-settings.txt");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=automart-settings.txt");
        send(exchange, 200, "text/plain; charset=utf-8", payload.getBytes(StandardCharsets.UTF_8));
    }

    static void sendCurrentUserExport(HttpExchange exchange) throws IOException {
        AppUser user = currentUser(exchange);
        if (user == null) {
            redirect(exchange, "/auth");
            return;
        }
        String payload = "Auto Mart User Details\n"
                + "Username: " + user.getUsername() + "\n"
                + "Role: " + user.getRole() + "\n"
                + "Email: " + user.getEmail() + "\n"
                + "Phone: " + user.getPhone() + "\n";
        exchange.getResponseHeaders().set("Content-Disposition",
                "attachment; filename=user-details-" + slug(user.getUsername()) + ".txt");
        send(exchange, 200, "text/plain; charset=utf-8", payload.getBytes(StandardCharsets.UTF_8));
    }

    static void sendAdminUsersFile(HttpExchange exchange) throws IOException {
        StringBuilder payload = new StringBuilder();
        for (AppUser user : loadUsers()) {
            payload.append(user.toRecord()).append("\n");
        }
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=auto-mart-users.txt");
        send(exchange, 200, "text/plain; charset=utf-8", payload.toString().getBytes(StandardCharsets.UTF_8));
    }

    static String userDetailsContent(HttpExchange exchange) throws IOException {
        AppUser user = currentUser(exchange);
        if (user == null) {
            return "<section class='container section'><div class='panel'><h1>No active user</h1><p>Please sign in first.</p></div></section>";
        }
        return "<section class='container section profile-page'><p class='eyebrow'>USER MANAGEMENT</p><h1>View User Details</h1>"
                + "<p class='lead'>Check saved account details before downloading the text file.</p>"
                + "<div class='panel user-details-card'><div class='profile-detail-grid'>"
                + "<article class='profile-mini-stat'><span>Username</span><strong>" + esc(user.getUsername())
                + "</strong></article>"
                + "<article class='profile-mini-stat'><span>Role</span><strong>" + esc(user.getRole())
                + "</strong></article>"
                + "<article class='profile-mini-stat'><span>Email</span><strong>" + esc(user.getEmail())
                + "</strong></article>"
                + "<article class='profile-mini-stat'><span>Phone</span><strong>" + esc(digitsOnly(user.getPhone()))
                + "</strong></article>"
                + "</div><div class='actions' style='margin-top:18px'><a class='btn btn-primary' href='/profile/export'>Download User Text File</a><a class='btn btn-secondary' href='/profile'>Back to Profile</a></div></div></section>";
    }

    static String settingsSectionOpen(String id, String title, String description) {
        return "<section id='" + esc(id)
                + "' class='panel settings-section reveal-up'><div class='section-head settings-section-head'><div><p class='eyebrow'>SECTION</p><h2>"
                + title + "</h2><p class='section-copy'>" + description + "</p></div></div>";
    }

    static String settingsSectionClose() {
        return "</section>";
    }

    static String settingsNavLink(String href, String icon, String label) {
        boolean external = href.startsWith("/");
        String attrs = external ? " data-route='page'" : "";
        return "<a class='settings-nav-link' href='" + href + "'" + attrs + "><span class='nav-icon'>" + icon
                + "</span><strong>" + esc(label) + "</strong></a>";
    }

    static String navIcon(String name) {
        return switch (name) {
            case "dashboard" ->
                "<svg viewBox='0 0 24 24' aria-hidden='true'><path d='M4 5.5h7v5H4zM13 5.5h7v8h-7zM4 12.5h7v6H4zM13 15.5h7v3h-7z'/></svg>";
            case "inventory" -> "<svg viewBox='0 0 24 24' aria-hidden='true'><path d='M5 7h14M5 12h14M5 17h14'/></svg>";
            case "sell" -> "<svg viewBox='0 0 24 24' aria-hidden='true'><path d='M12 5v14M5 12h14'/></svg>";
            case "requests" ->
                "<svg viewBox='0 0 24 24' aria-hidden='true'><path d='M7 7h10v10H7z'/><path d='M9 12h6'/></svg>";
            case "reviews" ->
                "<svg viewBox='0 0 24 24' aria-hidden='true'><path d='M12 4l2.3 4.7 5.2.8-3.8 3.7.9 5.3-4.6-2.4-4.6 2.4.9-5.3-3.8-3.7 5.2-.8z'/></svg>";
            case "users" ->
                "<svg viewBox='0 0 24 24' aria-hidden='true'><path d='M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm8 2a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z'/><path d='M4 19c0-2.8 2.7-5 6-5s6 2.2 6 5'/><path d='M15 19c.2-1.7 1.8-3 4-3 1 0 1.9.2 2.7.7'/></svg>";
            case "security" ->
                "<svg viewBox='0 0 24 24' aria-hidden='true'><path d='M12 4l7 3v5c0 4.4-2.7 7.8-7 9-4.3-1.2-7-4.6-7-9V7z'/><path d='M9.5 12.5l1.7 1.7 3.3-3.8'/></svg>";
            default ->
                "<svg viewBox='0 0 24 24' aria-hidden='true'><path d='M12 8v8M8 12h8'/><circle cx='12' cy='12' r='8'/></svg>";
        };
    }

    static String settingsMiniCard(String eyebrow, String text, String primary, String secondary, String kind) {
        return "<article class='settings-mini-card' data-kind='" + esc(kind) + "'><span class='mini-eyebrow'>"
                + esc(eyebrow) + "</span><h4 class='mini-card-title'>" + esc(text)
                + "</h4><div class='mini-card-actions'><button class='mini-action' type='button' data-action='edit'>"
                + esc(primary) + "</button><button class='mini-action danger-text' type='button' data-action='remove'>"
                + esc(secondary) + "</button></div></article>";
    }

    static String initials(String name) {
        String[] parts = safe(name).split("\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                out.append(Character.toUpperCase(part.charAt(0)));
            }
            if (out.length() == 2) {
                break;
            }
        }
        return out.length() == 0 ? "AM" : out.toString();
    }

    static String editVehicleContent(Vehicle v, String error) {
        String errorHtml = error == null ? "" : "<div class='alert'>" + esc(error) + "</div>";
        return "<section class='container section'><p class='eyebrow'>EDIT VEHICLE</p><h1>Update uploaded vehicle details</h1><p class='lead'>Use this work form to adjust listing details, move categories, or refresh the presentation image.</p>"
                + errorHtml
                + "<form class='sell-form panel edit-vehicle-form' method='post' action='/vehicle/edit'>"
                + hiddenField("id", v.id)
                + fieldWithValue("Vehicle title", "title", "Toyota RAV4 Adventure", v.title)
                + "<div class='form-grid'>"
                + selectField("Category", "category", optionsForCategory(v.category))
                + fieldWithValue("Price (LKR)", "price", "16500000", v.price)
                + fieldWithValue("Year", "year", "2021", v.year)
                + fieldWithValue("Mileage", "mileage", "42,000 km", v.mileage)
                + fieldWithValue("Fuel", "fuel", "Hybrid", v.fuel)
                + fieldWithValue("Transmission", "transmission", "Automatic", v.transmission)
                + "</div>"
                + fieldWithValue("Image path or URL", "image", "/assets/img/listings/suv-1.jpg", v.image)
                + textareaFieldWithValue("Sales description", "description", "Certified, inspected, and ready to roll.",
                        v.description)
                + "<div class='actions'><button class='btn btn-primary' type='submit'>Save Vehicle Changes</button><a class='btn btn-secondary' href='/settings'>Back to Work Settings</a><a class='btn btn-secondary' href='/vehicle?id="
                + url(v.id) + "'>View Vehicle</a></div></form></section>";
    }

    static String lockedVehicleContent(Vehicle v) {
        return "<section class='container section'><div class='panel'><p class='eyebrow'>MANAGEMENT LOCKED</p><h1>This vehicle is part of the seeded showroom catalog</h1><p class='lead'>Only uploaded vehicles can be edited or deleted from the work settings page. This protects the built-in inventory used for the demo experience.</p><div class='actions'><a class='btn btn-primary' href='/vehicle?id="
                + url(v.id)
                + "'>Back to Vehicle</a><a class='btn btn-secondary' href='/settings'>Open Settings</a></div></div></section>";
    }

    static String uploadsContent(String q) throws IOException {
        List<Vehicle> vehicles = loadVehicles();
        String query = safe(q).toLowerCase(Locale.ROOT);
        List<Vehicle> filtered = new ArrayList<>();
        for (Vehicle v : vehicles) {
            String haystack = (v.title + " " + v.category + " " + v.description + " " + v.fuel + " " + v.transmission)
                    .toLowerCase(Locale.ROOT);
            if (query.isBlank() || haystack.contains(query)) {
                filtered.add(v);
            }
        }
        StringBuilder items = new StringBuilder();
        for (Vehicle v : filtered.subList(0, Math.min(18, filtered.size()))) {
            items.append("<tr><td><a href='/vehicle?id=").append(url(v.id)).append("'>").append(esc(v.title))
                    .append("</a></td><td>")
                    .append(esc(displayCategory(v.category))).append("</td><td>")
                    .append(esc(formatLkr(v.price))).append("</td><td>")
                    .append(esc(formatTime(v.createdAt))).append("</td></tr>");
        }
        if (items.length() == 0) {
            items.append("<tr><td colspan='4' class='uploads-empty'>No recent uploads match your search.</td></tr>");
        }
        String search = "<form class='uploads-search reveal-up' method='get' action='/uploads'><label class='search-field uploads-search-field'><span>Search fresh arrivals</span><div class='uploads-search-row'><input name='q' value='"
                + esc(q)
                + "' placeholder='Search by vehicle name, category, fuel, or keyword'><button class='btn btn-primary' type='submit'>Search</button></div></label></form>";
        return "<section class='container section uploads-section'><div class='uploads-hero reveal-up'><p class='eyebrow'>RECENT UPLOADS</p><h1>Fresh Arrivals: Our Latest Premium Listings</h1><p class='lead uploads-lead'>Explore the newest premium vehicles added to Auto Mart, now with a quick search to help buyers find the right match faster.</p>"
                + search
                + "</div><div class='panel uploads-panel reveal-up'><table class='uploads-table'><thead><tr><th>Vehicle</th><th>Category</th><th>Price</th><th>Date &amp; Time</th></tr></thead><tbody>"
                + items + "</tbody></table></div></section>";
    }

    static String whyTrustUsContent() {
        StringBuilder html = new StringBuilder();
        html.append("<section class='container section trust-page'>");
        html.append("<div class='trust-hero panel'>");
        html.append(
                "<div><p class='eyebrow'>WHY TRUST US</p><h1>Confidence, clarity, and premium service at every step.</h1><p class='lead'>Auto Mart brings together inspected vehicles, verified paperwork, fair pricing guidance, and real customer support in one polished buying journey. We designed this experience to feel premium, clear, and dependable from your first click to final handover.</p><div class='actions'><a class='btn btn-primary' href='/inventory'>Browse Inventory</a><a class='btn btn-secondary' href='/sell'>Sell Your Car</a></div></div>");
        html.append(
                "<div class='trust-hero-visual'><div class='trust-hero-image-wrap'><img src='/assets/img/trust/inspection.jpg' alt='Premium inspected vehicle at Auto Mart'></div><div class='floating-chip chip-one'>Certified inspections</div><div class='floating-chip chip-two'>100% document support</div><div class='floating-chip chip-three'>Buyer-first guidance</div></div>");
        html.append("</div></section>");

        html.append("<section class='container section'>");
        html.append(
                "<div class='section-head'><div><p class='eyebrow'>TRUST &amp; QUALITY</p><h2>Our service standards protect your purchase</h2></div></div>");
        html.append("<div class='trust-feature-grid'>");
        html.append(trustFeatureCard("/assets/img/trust/inspection.jpg", "Multi-Point Mechanical Inspection",
                "Every listed vehicle goes through a rigorous mechanical review by qualified technicians, helping buyers shortlist with confidence and reduce hidden-risk surprises.",
                new String[] { "Engine &amp; drivetrain check", "Brake, suspension, and road-test review",
                        "Interior, electronics, and condition scoring" }));
        html.append(trustFeatureCard("/assets/img/trust/documents.jpg", "Verified Ownership Documents",
                "We help confirm registration details, ownership paperwork, and transfer readiness so the handover process stays legal, smooth, and stress-free.",
                new String[] { "Ownership and registration guidance", "Transfer-ready documentation checks",
                        "Reduced delays during handover" }));
        html.append(trustFeatureCard("/assets/img/trust/transparent-pricing.jpg", "Transparent Pricing &amp; Valuation",
                "The price you see is clearly presented in LKR, supported by realistic market positioning that matches current Sri Lankan automotive demand and vehicle condition.",
                new String[] { "No hidden processing surprises", "Market-aware LKR pricing",
                        "Clear value communication for buyers" }));
        html.append(trustFeatureCard("/assets/img/trust/customer-support.jpg", "Dedicated Buyer Support",
                "From first inquiry to delivery-day questions, our team keeps communication clear and responsive so buyers feel guided instead of pressured.",
                new String[] { "Prompt listing assistance", "Direct information support",
                        "Friendly after-sale follow-up" }));
        html.append("</div></section>");

        html.append("<section class='container section service-story'>");
        html.append(
                "<div class='service-copy panel'><p class='eyebrow'>OUR SERVICE</p><h2>More than listings — a polished second-hand car sale experience</h2><p>Auto Mart is built to give buyers a premium, practical, and transparent marketplace. Beyond displaying vehicles, we support smarter comparisons, clearer seller communication, and a more dependable purchasing process for Sri Lankan customers.</p><div class='service-bullets'><div><h3>Buyer benefits</h3><ul><li>Accident-history-aware presentation with stronger condition notes.</li><li>Well-structured model pages that make premium vehicles easier to compare.</li><li>Fast access to recent arrivals, category browsing, and detail-rich listings.</li></ul></div><div><h3>Seller benefits</h3><ul><li>Professional presentation that makes quality stock look more attractive.</li><li>Pricing guidance informed by current market expectations.</li><li>Trusted branding that helps serious buyers engage faster.</li></ul></div></div></div>");
        html.append(
                "<div class='service-timeline panel'><p class='eyebrow'>HOW WE HELP</p><div class='timeline-item'><span>01</span><div><h3>Vehicle sourcing &amp; inspection</h3><p>We highlight quality stock and strengthen confidence with inspection-driven descriptions.</p></div></div><div class='timeline-item'><span>02</span><div><h3>Paperwork &amp; price clarity</h3><p>We support legal transfer readiness and keep pricing communication straightforward.</p></div></div><div class='timeline-item'><span>03</span><div><h3>Buyer communication &amp; support</h3><p>We make it easier to understand each vehicle, ask questions, and move toward a confident purchase.</p></div></div><div class='timeline-item'><span>04</span><div><h3>After-sale reassurance</h3><p>Our service style is built around trust, follow-up, and a smoother ownership transition.</p></div></div></div>");
        html.append("</section>");
        return html.toString();
    }

    static String trustFeatureCard(String image, String title, String description, String[] points) {
        StringBuilder bulletHtml = new StringBuilder();
        for (String point : points) {
            bulletHtml.append("<li>").append(point).append("</li>");
        }
        return "<article class='trust-feature-card panel'><div class='trust-image-frame'><img src='" + image + "' alt='"
                + esc(title) + "'></div><div class='trust-feature-body'><h3>" + title + "</h3><p>" + description
                + "</p><ul>" + bulletHtml + "</ul></div></article>";
    }

    static void serveStatic(HttpExchange exchange, Path file) throws IOException {
        Path normalized = file.normalize();
        if (!normalized.startsWith(STATIC_DIR) && !normalized.startsWith(STATIC_DIR.resolve("."))) {
            notFound(exchange);
            return;
        }
        if (!Files.exists(normalized) || Files.isDirectory(normalized)) {
            notFound(exchange);
            return;
        }
        String type = contentType(normalized.getFileName().toString());
        send(exchange, 200, type, Files.readAllBytes(normalized));
    }

    static void send(HttpExchange ex, int code, String type, byte[] data) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", type);
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    static void html(HttpExchange ex, String html) throws IOException {
        send(ex, 200, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
    }

    static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    static void notFound(HttpExchange ex) throws IOException {
        send(ex, 404, "text/html; charset=utf-8", page(ex, "Not Found",
                "<section class='container section'><div class='panel'><h1>404</h1><p>The page you requested could not be found.</p></div></section>",
                true).getBytes(StandardCharsets.UTF_8));
    }

    static String page(HttpExchange exchange, String title, String content, boolean showChrome) {
        return TemplateRenderer.renderLayout(exchange, title, content, showChrome, "", "", "");
    }

    static String authPage(String error, boolean signUp) {
        return authPage(error, signUp, Map.of());
    }

    static String authPage(String error, boolean signUp, Map<String, String> values) {
        return TemplateRenderer.renderAuthPage(error, signUp, values);
    }

    static String socialButton(String provider, boolean signUp) {
        String label = Character.toUpperCase(provider.charAt(0)) + provider.substring(1);
        String icon = switch (provider) {
            case "google" -> "G";
            case "facebook" -> "f";
            default -> "in";
        };
        String mode = signUp ? "signup" : "signin";
        return "<a class='social-auth-btn social-" + provider + "' href='/social-auth?provider=" + provider + "&mode="
                + mode + "'><span class='social-auth-icon'>" + icon + "</span><span>"
                + (signUp ? "Sign up with " : "Sign in with ") + label + "</span></a>";
    }

    static String authFeatureCard(String title, String copy) {
        return "<article class='auth-feature-card'><strong>" + esc(title) + "</strong><p>" + esc(copy)
                + "</p></article>";
    }

    static String header(HttpExchange exchange) {
        return TemplateRenderer.renderHeader(exchange);
    }

    static String adminLoginPage(String error) {
        return adminLoginPage(error, "");
    }

    static String adminLoginPage(String error, String username) {
        return TemplateRenderer.renderAdminLoginPage(error, username);
    }

    static String adminSignupPage(String error, Map<String, String> values) {
        return TemplateRenderer.renderAdminSignupPage(error, values == null ? Map.of() : values);
    }

    static String footer() {
        return TemplateRenderer.renderFooter();
    }

    static String card(Vehicle v) {
        boolean longDescription = safe(v.description).length() > 210;
        String showMore = longDescription
                ? " <a class='show-more-link' href='/vehicle?id=" + url(v.id) + "'>Show more</a>"
                : "";
        String wishlistData = wishlistAttrs(v);
        return "<article class='card animated-card premium-wishlist-card' data-tilt-card>"
                + "<div class='card-media-shell'><img src='" + imageSrc(v.image) + "' alt='" + esc(v.title)
                + "'><button class='wishlist-btn floating-wishlist-btn' type='button' " + wishlistData
                + " aria-label='Save to wishlist'><span class='wishlist-btn-heart'>♡</span><span class='wishlist-btn-text'>Save</span></button><div class='card-shine'></div></div>"
                + "<div class='card-body'><div class='badge-row'><span class='badge'>"
                + esc(displayCategory(v.category)) + "</span><span class='badge soft'>" + esc(v.year)
                + "</span></div><h3>" + esc(v.title) + "</h3><p class='card-description'>"
                + esc(shortDescription(v.description, 210)) + showMore + "</p><div class='mini-specs'><span>"
                + esc(v.mileage) + "</span><span>" + esc(v.fuel) + "</span><span>" + esc(v.transmission)
                + "</span></div><div class='wishlist-action-row'><button class='wishlist-chip-btn' type='button' "
                + wishlistData + ">Add to Wishlist</button><a class='wishlist-ghost-link' href='/vehicle?id="
                + url(v.id) + "'>View Details</a></div><div class='card-bottom'><strong>" + esc(formatLkr(v.price))
                + "</strong><span class='wishlist-status' data-wishlist-status='" + esc(v.id)
                + "'>Ready to save</span></div></div></article>";
    }

    static String wishlistAttrs(Vehicle v) {
        return "data-wishlist-id='" + esc(v.id) + "' "
                + "data-wishlist-title='" + esc(v.title) + "' "
                + "data-wishlist-price='" + esc(formatLkr(v.price)) + "' "
                + "data-wishlist-image='" + esc(imageSrc(v.image)) + "' "
                + "data-wishlist-category='" + esc(displayCategory(v.category)) + "' "
                + "data-wishlist-year='" + esc(v.year) + "' "
                + "data-wishlist-mileage='" + esc(v.mileage) + "' "
                + "data-wishlist-fuel='" + esc(v.fuel) + "' "
                + "data-wishlist-transmission='" + esc(v.transmission) + "' "
                + "data-wishlist-link='/vehicle?id=" + url(v.id) + "'";
    }

    static String wishlistContent() {
        return "<section class='container section wishlist-page'>"
                + "<div class='wishlist-hero panel animated-card'>"
                + "<div class='wishlist-hero-copy'><p class='eyebrow'>PREMIUM WISHLIST</p><h1>Your saved dream garage</h1><p class='lead'>Shortlist the right second-hand cars, revisit them later, and move toward offers, inspections, and test drives with a more realistic premium flow.</p><div class='wishlist-hero-actions'><a class='btn btn-primary premium-glow-btn' href='/inventory'>Browse cars</a><a class='btn btn-secondary' href='/requests'>Open requests</a></div></div>"
                + "<div class='wishlist-hero-stats'><article><span>Saved cars</span><strong data-wishlist-total>0</strong><small>Live shortlist</small></article><article><span>Ready to compare</span><strong data-wishlist-compare>0</strong><small>Best matches</small></article><article><span>Premium flow</span><strong>Offer / Reserve</strong><small>No cart needed</small></article></div>"
                + "</div>"
                + "<div class='wishlist-showcase-grid'>"
                + "<article class='panel wishlist-intelligence-card animated-card'><span class='wishlist-section-badge'>Why wishlist works better</span><h3>Designed for real used-car buying</h3><p>Save favorites, compare them mentally, then move into test drives, requests, and negotiation instead of a normal ecommerce checkout.</p><div class='wishlist-pill-row'><span>Save Favorites</span><span>Revisit Later</span><span>Make Offer</span><span>Book Test Drive</span></div></article>"
                + "<article class='panel wishlist-intelligence-card animated-card'><span class='wishlist-section-badge'>Next best actions</span><h3>Turn saved vehicles into action</h3><p>Every saved car stays ready for an inspection request, a seller chat, or a reserve-now style conversion later in your workflow.</p><div class='wishlist-orbital'><span></span><span></span><span></span></div></article>"
                + "</div>"
                + "<div class='wishlist-board panel'>"
                + "<div class='wishlist-board-head'><div><p class='eyebrow'>SHORTLIST BOARD</p><h2>Saved vehicles</h2></div><button class='btn btn-secondary wishlist-clear-btn' type='button' data-wishlist-clear>Clear wishlist</button></div>"
                + "<div id='wishlist-grid' class='wishlist-grid'></div>"
                + "<div id='wishlist-empty' class='wishlist-empty-state'><div class='wishlist-empty-orbit'></div><h3>Your wishlist is empty</h3><p>Save vehicles from Inventory or the vehicle detail page to build a premium shortlist.</p><a class='btn btn-primary' href='/inventory'>Explore inventory</a></div>"
                + "</div>"
                + "</section>";
    }

    static String tab(String c, String selected, String q, String fuel, String transmission, String sort,
            String label) {
        return "<a class='tab " + (c.equals(selected) ? "active" : "") + "' href='/inventory?category=" + url(c)
                + "&q=" + url(q)
                + "&fuel=" + url(fuel)
                + "&transmission=" + url(transmission)
                + "&sort=" + url(sort) + "'>" + label + "</a>";
    }

    static String spec(String label, String value) {
        return "<div class='spec'><span>" + esc(label) + "</span><strong>" + esc(value) + "</strong></div>";
    }

    static String shortDescription(String description, int limit) {
        String clean = safe(description).replace("\r", " ").replace("\n", " ").replaceAll("\\s+", " ").trim();
        if (clean.length() <= limit) {
            return clean;
        }
        int cut = clean.lastIndexOf(' ', Math.max(0, limit - 3));
        if (cut < limit / 2) {
            cut = limit - 3;
        }
        return clean.substring(0, Math.max(0, cut)).trim() + "...";
    }

    static String formatDescriptionHtml(String description) {
        return esc(safe(description)).replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br>");
    }

    static boolean isAuthenticated(HttpExchange exchange) {
        return currentUser(exchange) != null;
    }

    static void startSession(HttpExchange exchange, String username) {
        SessionManager.startSession(exchange, username);
    }

    static void clearSession(HttpExchange exchange) {
        SessionManager.clearSession(exchange);
    }

    static String readCookie(HttpExchange exchange, String name) {
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");
        if (cookieHeaders == null) {
            return "";
        }
        for (String header : cookieHeaders) {
            for (String entry : header.split(";")) {
                String[] parts = entry.trim().split("=", 2);
                if (parts.length == 2 && parts[0].trim().equals(name)) {
                    return decode(parts[1].trim());
                }
            }
        }
        return "";
    }

    static boolean matchesSearch(Vehicle v, String query) {
        String needle = safe(query).toLowerCase(Locale.ROOT);
        if (needle.isBlank()) {
            return true;
        }
        String haystack = String.join(" ", v.title, v.description, v.category, v.fuel, v.transmission, v.year)
                .toLowerCase(Locale.ROOT);
        return haystack.contains(needle);
    }

    static void sortVehicles(List<Vehicle> vehicles, String sort) {
        if ("price-low".equals(sort) || "price-high".equals(sort)) {
            VehicleLinkedList linkedList = new VehicleLinkedList();
            for (Vehicle vehicle : vehicles) {
                linkedList.insert(vehicle);
            }
            List<Vehicle> sorted = linkedList.mergeSortByPrice("price-high".equals(sort));
            vehicles.clear();
            vehicles.addAll(sorted);
            return;
        }
        switch (sort) {
            case "year-new" -> vehicles.sort(Comparator.comparingInt((Vehicle v) -> parseYearValue(v.year)).reversed());
            case "year-old" -> vehicles.sort(Comparator.comparingInt(v -> parseYearValue(v.year)));
            default -> vehicles.sort(Comparator.comparing((Vehicle v) -> v.createdAt).reversed());
        }
    }

    static long parsePriceValue(String raw) {
        try {
            return Long.parseLong(normalizePriceLkr(raw));
        } catch (Exception e) {
            return 0L;
        }
    }

    static int parseYearValue(String raw) {
        try {
            return Integer.parseInt(safe(raw).replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    static String optionsForFilter(List<String> values, String selected) {
        StringBuilder out = new StringBuilder("<option value='all'>All</option>");
        for (String value : values) {
            out.append("<option value='").append(esc(value)).append("'");
            if (value.equalsIgnoreCase(selected)) {
                out.append(" selected");
            }
            out.append(">").append(esc(value)).append("</option>");
        }
        return out.toString();
    }

    static String sortOptions(String selected) {
        return sortOption("latest", selected, "Latest")
                + sortOption("price-low", selected, "Price: Low to High")
                + sortOption("price-high", selected, "Price: High to Low")
                + sortOption("year-new", selected, "Year: Newest First")
                + sortOption("year-old", selected, "Year: Oldest First");
    }

    static String sortOption(String value, String selected, String label) {
        return "<option value='" + value + "'" + (value.equals(selected) ? " selected" : "") + ">" + label
                + "</option>";
    }

    static String normalizeInventoryCategory(String category) {
        String v = safe(category).toLowerCase(Locale.ROOT);
        return "all".equals(v) ? "all" : normalizeCategory(v);
    }

    static String normalizeFilterValue(String value, List<String> allowed) {
        String v = safe(value);
        if (v.isBlank() || "all".equalsIgnoreCase(v)) {
            return "all";
        }
        for (String option : allowed) {
            if (option.equalsIgnoreCase(v)) {
                return option;
            }
        }
        return "all";
    }

    static String normalizeSort(String sort) {
        List<String> allowed = Arrays.asList("latest", "price-low", "price-high", "year-new", "year-old");
        String v = safe(sort).toLowerCase(Locale.ROOT);
        return allowed.contains(v) ? v : "latest";
    }

    static List<String> fuels() {
        return Arrays.asList("Petrol", "Diesel", "Hybrid", "Electric");
    }

    static List<String> transmissions() {
        return Arrays.asList("Automatic", "Manual");
    }

    static String validationAttrs(String name) {
        String key = safe(name).toLowerCase(Locale.ROOT);
        if (key.contains("email"))
            return " type='email' pattern='^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$' title='Use a valid email. Do not use # or %.'";
        if (key.contains("phone") || key.contains("whatsapp"))
            return " inputmode='numeric' maxlength='10' pattern='[0-9]{7,10}' title='Use digits only, maximum 10 digits.'";
        return "";
    }

    static String field(String label, String name, String placeholder) {
        return "<label>" + esc(label) + "<input name='" + esc(name) + "' placeholder='" + esc(placeholder) + "'"
                + validationAttrs(name) + " required></label>";
    }

    static String fieldWithValue(String label, String name, String placeholder, String value) {
        return "<label>" + esc(label) + "<input name='" + esc(name) + "' placeholder='" + esc(placeholder) + "' value='"
                + esc(value) + "'" + validationAttrs(name) + " required></label>";
    }

    static String selectField(String label, String name, String options) {
        return "<label>" + esc(label) + "<select name='" + esc(name) + "'>" + options + "</select></label>";
    }

    static String textareaField(String label, String name, String placeholder) {
        return "<label>" + esc(label) + "<textarea name='" + esc(name) + "' rows='5' placeholder='" + esc(placeholder)
                + "'></textarea></label>";
    }

    static String textareaFieldWithValue(String label, String name, String placeholder, String value) {
        return "<label>" + esc(label) + "<textarea name='" + esc(name) + "' rows='5' placeholder='" + esc(placeholder)
                + "'>" + esc(value) + "</textarea></label>";
    }

    static String hiddenField(String name, String value) {
        return "<input type='hidden' name='" + esc(name) + "' value='" + esc(value) + "'>";
    }

    static String optionsForCategory(String selected) {
        StringBuilder opts = new StringBuilder();
        for (String c : categories()) {
            opts.append("<option value='").append(c).append("'");
            if (c.equals(selected)) {
                opts.append(" selected");
            }
            opts.append(">")
                    .append(displayCategory(c))
                    .append("</option>");
        }
        return opts.toString();
    }

    static String selectOptions(String[][] options, String selected) {
        StringBuilder out = new StringBuilder();
        for (String[] option : options) {
            out.append("<option value='").append(option[0]).append("'");
            if (option[0].equals(selected)) {
                out.append(" selected");
            }
            out.append(">")
                    .append(option[1])
                    .append("</option>");
        }
        return out.toString();
    }

    static String toggle(String label, String name, boolean checked) {
        return "<label class='setting-toggle'><input type='checkbox' name='" + esc(name) + "'"
                + (checked ? " checked" : "") + "><span class='setting-toggle-ui'></span><strong>" + esc(label)
                + "</strong></label>";
    }

    static String premiumCard(String title, String description, String tier, boolean enabled) {
        return "<label class='premium-option animated-card'><input type='checkbox' name='"
                + esc(title.toLowerCase(Locale.ROOT).replace(' ', '-')) + "' class='premium-check'"
                + (enabled ? " checked" : "") + "><span class='premium-tier'>" + esc(tier) + "</span><h3>" + esc(title)
                + "</h3><p>" + esc(description) + "</p><span class='premium-status'>"
                + (enabled ? "Enabled" : "Tap to enable") + "</span></label>";
    }

    static String infoTile(String label, String value) {
        return "<div class='info-tile'><span>" + esc(label) + "</span><strong>" + esc(value) + "</strong></div>";
    }

    static Settings readSettings(HttpExchange exchange) {
        String cookie = readCookie(exchange, SETTINGS_COOKIE);
        Settings s = new Settings();
        if (cookie.isBlank()) {
            return s;
        }
        for (String part : cookie.split("\\|")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            String value = decode(pair[1]);
            switch (pair[0]) {
                case "fullName" -> s.fullName = value;
                case "profilePhoto" -> s.profilePhoto = value;
                case "premiumBadgeStatus" -> s.premiumBadgeStatus = value;
                case "teamEmail" -> s.teamEmail = value;
                case "phone" -> s.phone = value;
                case "location" -> s.location = value;
                case "bio" -> s.bio = value;
                case "defaultCity" -> s.defaultCity = value;
                case "workspaceMode" -> s.workspaceMode = value;
                case "defaultDashboard" -> s.defaultDashboard = value;
                case "notificationFrequency" -> s.notificationFrequency = value;
                case "preferredContact" -> s.preferredContact = value;
                case "theme" -> s.theme = value;
                case "distanceUnit" -> s.distanceUnit = value;
                case "currency" -> s.currency = value;
                case "preferredLocationFirst" -> s.preferredLocationFirst = value;
                case "defaultPickup" -> s.defaultPickup = value;
                case "businessHours" -> s.businessHours = value;
                case "awayMessage" -> s.awayMessage = value;
                case "blockedUsers" -> s.blockedUsers = value;
                case "subscriptionPlan" -> s.subscriptionPlan = value;
                case "featuredCredits" -> s.featuredCredits = value;
                case "billingEmail" -> s.billingEmail = value;
                case "dealerName" -> s.dealerName = value;
                case "whatsappNumber" -> s.whatsappNumber = value;
                case "registrationNumber" -> s.registrationNumber = value;
                case "businessAddress" -> s.businessAddress = value;
                case "sellerType" -> s.sellerType = value;
                case "mapsUrl" -> s.mapsUrl = value;
                case "warrantyNote" -> s.warrantyNote = value;
                case "financePartner" -> s.financePartner = value;
                case "tradeInPolicy" -> s.tradeInPolicy = value;
                case "responseSla" -> s.responseSla = value;
                case "wishlistSpotlight" -> s.wishlistSpotlight = value;
                case "priceType" -> s.priceType = value;
                case "listingDuration" -> s.listingDuration = value;
                case "minPhotos" -> s.minPhotos = value;
                case "deliveryFee" -> s.deliveryFee = value;
                case "testDriveSlot" -> s.testDriveSlot = value;
                case "paymentMethod" -> s.paymentMethod = value;
                case "bankName" -> s.bankName = value;
                case "accountName" -> s.accountName = value;
                case "accountNumber" -> s.accountNumber = value;
                case "welcomeHeadline" -> s.welcomeHeadline = value;
                case "welcomeSubtext" -> s.welcomeSubtext = value;
                case "defaultSignupRole" -> s.defaultSignupRole = value;
                case "planNote" -> s.planNote = value;
                case "verifiedSeller" -> s.verifiedSeller = Boolean.parseBoolean(value);
                case "smsAlerts" -> s.smsAlerts = Boolean.parseBoolean(value);
                case "pushAlerts" -> s.pushAlerts = Boolean.parseBoolean(value);
                case "priceDropAlerts" -> s.priceDropAlerts = Boolean.parseBoolean(value);
                case "savedSearchAlerts" -> s.savedSearchAlerts = Boolean.parseBoolean(value);
                case "chatNotifications" -> s.chatNotifications = Boolean.parseBoolean(value);
                case "emailNotifications" -> s.emailNotifications = Boolean.parseBoolean(value);
                case "conciergeMode" -> s.conciergeMode = Boolean.parseBoolean(value);
                case "inventorySync" -> s.inventorySync = Boolean.parseBoolean(value);
                case "twoFactorAuth" -> s.twoFactorAuth = Boolean.parseBoolean(value);
                case "privacyMode" -> s.privacyMode = Boolean.parseBoolean(value);
                case "sessionShield" -> s.sessionShield = Boolean.parseBoolean(value);
                case "showPhoneNumber" -> s.showPhoneNumber = Boolean.parseBoolean(value);
                case "governmentIdUploaded" -> s.governmentIdUploaded = Boolean.parseBoolean(value);
                case "dataExportReady" -> s.dataExportReady = Boolean.parseBoolean(value);
                case "autoRenew" -> s.autoRenew = Boolean.parseBoolean(value);
                case "hidePhone" -> s.hidePhone = Boolean.parseBoolean(value);
                case "automatedReplies" -> s.automatedReplies = Boolean.parseBoolean(value);
                case "googleVehicleAds" -> s.googleVehicleAds = Boolean.parseBoolean(value);
                case "dmsSync" -> s.dmsSync = Boolean.parseBoolean(value);
                case "featuredBoost" -> s.featuredBoost = Boolean.parseBoolean(value);
                case "inspectionAvailable" -> s.inspectionAvailable = Boolean.parseBoolean(value);
                case "serviceHistoryEnabled" -> s.serviceHistoryEnabled = Boolean.parseBoolean(value);
                case "vinEnabled" -> s.vinEnabled = Boolean.parseBoolean(value);
                case "financeAvailable" -> s.financeAvailable = Boolean.parseBoolean(value);
                case "tradeInEnabled" -> s.tradeInEnabled = Boolean.parseBoolean(value);
                case "warrantyEnabled" -> s.warrantyEnabled = Boolean.parseBoolean(value);
                case "documentVerification" -> s.documentVerification = Boolean.parseBoolean(value);
                case "accidentHistoryEnabled" -> s.accidentHistoryEnabled = Boolean.parseBoolean(value);
                case "islandwideDelivery" -> s.islandwideDelivery = Boolean.parseBoolean(value);
                case "homeTestDrive" -> s.homeTestDrive = Boolean.parseBoolean(value);
                case "previousOwnersEnabled" -> s.previousOwnersEnabled = Boolean.parseBoolean(value);
                case "allowEmailLogin" -> s.allowEmailLogin = Boolean.parseBoolean(value);
                case "socialGoogle" -> s.socialGoogle = Boolean.parseBoolean(value);
                case "socialFacebook" -> s.socialFacebook = Boolean.parseBoolean(value);
                case "socialLinkedin" -> s.socialLinkedin = Boolean.parseBoolean(value);
                case "socialSignup" -> s.socialSignup = Boolean.parseBoolean(value);
                case "animatedAuth" -> s.animatedAuth = Boolean.parseBoolean(value);
            }
        }
        return s;
    }

    static void saveSettings(HttpExchange exchange, Map<String, String> form) {
        Settings s = new Settings();
        s.fullName = safe(form.getOrDefault("fullName", s.fullName));
        s.profilePhoto = safe(form.getOrDefault("profilePhoto", s.profilePhoto));
        s.premiumBadgeStatus = safe(form.getOrDefault("premiumBadgeStatus", s.premiumBadgeStatus));
        s.teamEmail = safe(form.getOrDefault("teamEmail", s.teamEmail));
        s.phone = safe(form.getOrDefault("phone", s.phone));
        s.location = safe(form.getOrDefault("location", s.location));
        s.bio = safe(form.getOrDefault("bio", s.bio));
        s.defaultCity = safe(form.getOrDefault("defaultCity", s.defaultCity));
        s.workspaceMode = safe(form.getOrDefault("workspaceMode", s.workspaceMode));
        s.defaultDashboard = safe(form.getOrDefault("defaultDashboard", s.defaultDashboard));
        s.notificationFrequency = safe(form.getOrDefault("notificationFrequency", s.notificationFrequency));
        s.preferredContact = safe(form.getOrDefault("preferredContact", s.preferredContact));
        s.theme = safe(form.getOrDefault("theme", s.theme));
        s.distanceUnit = safe(form.getOrDefault("distanceUnit", s.distanceUnit));
        s.currency = safe(form.getOrDefault("currency", s.currency));
        s.preferredLocationFirst = safe(form.getOrDefault("preferredLocationFirst", s.preferredLocationFirst));
        s.defaultPickup = safe(form.getOrDefault("defaultPickup", s.defaultPickup));
        s.businessHours = safe(form.getOrDefault("businessHours", s.businessHours));
        s.awayMessage = safe(form.getOrDefault("awayMessage", s.awayMessage));
        s.blockedUsers = safe(form.getOrDefault("blockedUsers", s.blockedUsers));
        s.subscriptionPlan = safe(form.getOrDefault("subscriptionPlan", s.subscriptionPlan));
        s.featuredCredits = safe(form.getOrDefault("featuredCredits", s.featuredCredits));
        s.billingEmail = safe(form.getOrDefault("billingEmail", s.billingEmail));
        s.dealerName = safe(form.getOrDefault("dealerName", s.dealerName));
        s.whatsappNumber = safe(form.getOrDefault("whatsappNumber", s.whatsappNumber));
        s.registrationNumber = safe(form.getOrDefault("registrationNumber", s.registrationNumber));
        s.businessAddress = safe(form.getOrDefault("businessAddress", s.businessAddress));
        s.sellerType = safe(form.getOrDefault("sellerType", s.sellerType));
        s.mapsUrl = safe(form.getOrDefault("mapsUrl", s.mapsUrl));
        s.warrantyNote = safe(form.getOrDefault("warrantyNote", s.warrantyNote));
        s.financePartner = safe(form.getOrDefault("financePartner", s.financePartner));
        s.tradeInPolicy = safe(form.getOrDefault("tradeInPolicy", s.tradeInPolicy));
        s.responseSla = safe(form.getOrDefault("responseSla", s.responseSla));
        s.wishlistSpotlight = safe(form.getOrDefault("wishlistSpotlight", s.wishlistSpotlight));
        s.priceType = safe(form.getOrDefault("priceType", s.priceType));
        s.listingDuration = safe(form.getOrDefault("listingDuration", s.listingDuration));
        s.minPhotos = safe(form.getOrDefault("minPhotos", s.minPhotos));
        s.deliveryFee = safe(form.getOrDefault("deliveryFee", s.deliveryFee));
        s.testDriveSlot = safe(form.getOrDefault("testDriveSlot", s.testDriveSlot));
        s.paymentMethod = safe(form.getOrDefault("paymentMethod", s.paymentMethod));
        s.bankName = safe(form.getOrDefault("bankName", s.bankName));
        s.accountName = safe(form.getOrDefault("accountName", s.accountName));
        s.accountNumber = safe(form.getOrDefault("accountNumber", s.accountNumber));
        s.welcomeHeadline = safe(form.getOrDefault("welcomeHeadline", s.welcomeHeadline));
        s.welcomeSubtext = safe(form.getOrDefault("welcomeSubtext", s.welcomeSubtext));
        s.defaultSignupRole = safe(form.getOrDefault("defaultSignupRole", s.defaultSignupRole));
        s.planNote = safe(form.getOrDefault("planNote", s.planNote));
        s.verifiedSeller = form.containsKey("verifiedSeller");
        s.smsAlerts = form.containsKey("smsAlerts");
        s.pushAlerts = form.containsKey("pushAlerts");
        s.priceDropAlerts = form.containsKey("priceDropAlerts");
        s.savedSearchAlerts = form.containsKey("savedSearchAlerts");
        s.chatNotifications = form.containsKey("chatNotifications");
        s.emailNotifications = form.containsKey("emailNotifications");
        s.conciergeMode = form.containsKey("conciergeMode") || form.containsKey("concierge-mode");
        s.inventorySync = form.containsKey("inventorySync");
        s.twoFactorAuth = form.containsKey("twoFactorAuth");
        s.privacyMode = form.containsKey("privacyMode");
        s.sessionShield = form.containsKey("sessionShield");
        s.showPhoneNumber = form.containsKey("showPhoneNumber");
        s.governmentIdUploaded = form.containsKey("governmentIdUploaded");
        s.dataExportReady = form.containsKey("dataExportReady");
        s.autoRenew = form.containsKey("autoRenew");
        s.hidePhone = form.containsKey("hidePhone");
        s.automatedReplies = form.containsKey("automatedReplies");
        s.googleVehicleAds = form.containsKey("googleVehicleAds");
        s.dmsSync = form.containsKey("dmsSync");
        s.featuredBoost = form.containsKey("featuredBoost");
        s.inspectionAvailable = form.containsKey("inspectionAvailable");
        s.serviceHistoryEnabled = form.containsKey("serviceHistoryEnabled");
        s.vinEnabled = form.containsKey("vinEnabled");
        s.financeAvailable = form.containsKey("financeAvailable");
        s.tradeInEnabled = form.containsKey("tradeInEnabled");
        s.warrantyEnabled = form.containsKey("warrantyEnabled");
        s.documentVerification = form.containsKey("documentVerification");
        s.accidentHistoryEnabled = form.containsKey("accidentHistoryEnabled");
        s.islandwideDelivery = form.containsKey("islandwideDelivery");
        s.homeTestDrive = form.containsKey("homeTestDrive");
        s.previousOwnersEnabled = form.containsKey("previousOwnersEnabled");
        s.allowEmailLogin = form.containsKey("allowEmailLogin");
        s.socialGoogle = form.containsKey("socialGoogle");
        s.socialFacebook = form.containsKey("socialFacebook");
        s.socialLinkedin = form.containsKey("socialLinkedin");
        s.socialSignup = form.containsKey("socialSignup");
        s.animatedAuth = form.containsKey("animatedAuth");
        List<String> pairs = new ArrayList<>();
        pairs.add("fullName=" + url(s.fullName));
        pairs.add("profilePhoto=" + url(s.profilePhoto));
        pairs.add("premiumBadgeStatus=" + url(s.premiumBadgeStatus));
        pairs.add("teamEmail=" + url(s.teamEmail));
        pairs.add("phone=" + url(s.phone));
        pairs.add("location=" + url(s.location));
        pairs.add("bio=" + url(s.bio));
        pairs.add("defaultCity=" + url(s.defaultCity));
        pairs.add("workspaceMode=" + url(s.workspaceMode));
        pairs.add("defaultDashboard=" + url(s.defaultDashboard));
        pairs.add("notificationFrequency=" + url(s.notificationFrequency));
        pairs.add("preferredContact=" + url(s.preferredContact));
        pairs.add("theme=" + url(s.theme));
        pairs.add("distanceUnit=" + url(s.distanceUnit));
        pairs.add("currency=" + url(s.currency));
        pairs.add("preferredLocationFirst=" + url(s.preferredLocationFirst));
        pairs.add("defaultPickup=" + url(s.defaultPickup));
        pairs.add("businessHours=" + url(s.businessHours));
        pairs.add("awayMessage=" + url(s.awayMessage));
        pairs.add("blockedUsers=" + url(s.blockedUsers));
        pairs.add("subscriptionPlan=" + url(s.subscriptionPlan));
        pairs.add("featuredCredits=" + url(s.featuredCredits));
        pairs.add("billingEmail=" + url(s.billingEmail));
        pairs.add("dealerName=" + url(s.dealerName));
        pairs.add("whatsappNumber=" + url(s.whatsappNumber));
        pairs.add("registrationNumber=" + url(s.registrationNumber));
        pairs.add("businessAddress=" + url(s.businessAddress));
        pairs.add("sellerType=" + url(s.sellerType));
        pairs.add("mapsUrl=" + url(s.mapsUrl));
        pairs.add("warrantyNote=" + url(s.warrantyNote));
        pairs.add("financePartner=" + url(s.financePartner));
        pairs.add("tradeInPolicy=" + url(s.tradeInPolicy));
        pairs.add("responseSla=" + url(s.responseSla));
        pairs.add("wishlistSpotlight=" + url(s.wishlistSpotlight));
        pairs.add("priceType=" + url(s.priceType));
        pairs.add("listingDuration=" + url(s.listingDuration));
        pairs.add("minPhotos=" + url(s.minPhotos));
        pairs.add("deliveryFee=" + url(s.deliveryFee));
        pairs.add("testDriveSlot=" + url(s.testDriveSlot));
        pairs.add("paymentMethod=" + url(s.paymentMethod));
        pairs.add("bankName=" + url(s.bankName));
        pairs.add("accountName=" + url(s.accountName));
        pairs.add("accountNumber=" + url(s.accountNumber));
        pairs.add("welcomeHeadline=" + url(s.welcomeHeadline));
        pairs.add("welcomeSubtext=" + url(s.welcomeSubtext));
        pairs.add("defaultSignupRole=" + url(s.defaultSignupRole));
        pairs.add("planNote=" + url(s.planNote));
        pairs.add("verifiedSeller=" + s.verifiedSeller);
        pairs.add("smsAlerts=" + s.smsAlerts);
        pairs.add("pushAlerts=" + s.pushAlerts);
        pairs.add("priceDropAlerts=" + s.priceDropAlerts);
        pairs.add("savedSearchAlerts=" + s.savedSearchAlerts);
        pairs.add("chatNotifications=" + s.chatNotifications);
        pairs.add("emailNotifications=" + s.emailNotifications);
        pairs.add("conciergeMode=" + s.conciergeMode);
        pairs.add("inventorySync=" + s.inventorySync);
        pairs.add("twoFactorAuth=" + s.twoFactorAuth);
        pairs.add("privacyMode=" + s.privacyMode);
        pairs.add("sessionShield=" + s.sessionShield);
        pairs.add("showPhoneNumber=" + s.showPhoneNumber);
        pairs.add("governmentIdUploaded=" + s.governmentIdUploaded);
        pairs.add("dataExportReady=" + s.dataExportReady);
        pairs.add("autoRenew=" + s.autoRenew);
        pairs.add("hidePhone=" + s.hidePhone);
        pairs.add("automatedReplies=" + s.automatedReplies);
        pairs.add("googleVehicleAds=" + s.googleVehicleAds);
        pairs.add("dmsSync=" + s.dmsSync);
        pairs.add("featuredBoost=" + s.featuredBoost);
        pairs.add("inspectionAvailable=" + s.inspectionAvailable);
        pairs.add("serviceHistoryEnabled=" + s.serviceHistoryEnabled);
        pairs.add("vinEnabled=" + s.vinEnabled);
        pairs.add("financeAvailable=" + s.financeAvailable);
        pairs.add("tradeInEnabled=" + s.tradeInEnabled);
        pairs.add("warrantyEnabled=" + s.warrantyEnabled);
        pairs.add("documentVerification=" + s.documentVerification);
        pairs.add("accidentHistoryEnabled=" + s.accidentHistoryEnabled);
        pairs.add("islandwideDelivery=" + s.islandwideDelivery);
        pairs.add("homeTestDrive=" + s.homeTestDrive);
        pairs.add("previousOwnersEnabled=" + s.previousOwnersEnabled);
        pairs.add("allowEmailLogin=" + s.allowEmailLogin);
        pairs.add("socialGoogle=" + s.socialGoogle);
        pairs.add("socialFacebook=" + s.socialFacebook);
        pairs.add("socialLinkedin=" + s.socialLinkedin);
        pairs.add("socialSignup=" + s.socialSignup);
        pairs.add("animatedAuth=" + s.animatedAuth);
        exchange.getResponseHeaders().add("Set-Cookie",
                SETTINGS_COOKIE + "=" + String.join("|", pairs) + "; Path=/; Max-Age=2592000; SameSite=Lax");
    }

    static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return map;
        }
        for (String part : raw.split("&")) {
            int i = part.indexOf('=');
            if (i > -1) {
                map.put(decode(part.substring(0, i)), decode(part.substring(i + 1)));
            } else {
                map.put(decode(part), "");
            }
        }
        return map;
    }

    static Map<String, String> parseForm(byte[] bytes) {
        return parseQuery(new String(bytes, StandardCharsets.UTF_8));
    }

    static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    static String contentType(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (lower.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".mp4")) {
            return "video/mp4";
        }
        return "application/octet-stream";
    }

    static String normalizeSubmittedImage(String category, String image) {
        String value = safe(image);
        return value.isBlank() ? defaultImage(category) : value;
    }

    static String imageSrc(String image) {
        String value = safe(image);
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return "/image-proxy?url=" + encodeUriComponent(value);
        }
        return esc(value);
    }

    static String encodeUriComponent(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_'
                    || c == '.' || c == '~') {
                out.append((char) c);
            } else {
                out.append('%');
                String hex = Integer.toHexString(c).toUpperCase(Locale.ROOT);
                if (hex.length() == 1) {
                    out.append('0');
                }
                out.append(hex);
            }
        }
        return out.toString();
    }

    static void serveRemoteImage(HttpExchange exchange, String rawUrl) throws IOException {
        String target = safe(rawUrl);
        if (!(target.startsWith("http://") || target.startsWith("https://"))) {
            notFound(exchange);
            return;
        }
        HttpURLConnection connection = null;
        try {
            URL url = URI.create(target).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 AutoMart/1.0");
            connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
            int status = connection.getResponseCode();
            if (status >= 300 && status < 400) {
                String location = connection.getHeaderField("Location");
                if (location != null && !location.isBlank()) {
                    serveRemoteImage(exchange, location);
                    return;
                }
            }
            if (status >= 400) {
                notFound(exchange);
                return;
            }
            byte[] bytes = connection.getInputStream().readAllBytes();
            String contentType = safe(connection.getContentType());
            if (contentType.isBlank()) {
                contentType = guessRemoteContentType(target);
            }
            send(exchange, 200, contentType, bytes);
        } catch (Exception e) {
            notFound(exchange);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static String guessRemoteContentType(String url) {
        String lower = safe(url).toLowerCase(Locale.ROOT);
        if (lower.contains(".webp")) {
            return "image/webp";
        }
        if (lower.contains(".png")) {
            return "image/png";
        }
        if (lower.contains(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }

    static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    static String esc(String s) {
        return safe(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    static String url(String s) {
        return esc(s).replace(" ", "%20");
    }

    static String normalizeCategory(String c) {
        String v = safe(c).toLowerCase(Locale.ROOT);
        for (String allowed : categories()) {
            if (allowed.equals(v)) {
                return v;
            }
        }
        return "suv";
    }

    static String displayCategory(String category) {
        return switch (category) {
            case "crossover" -> "Crossover";
            case "sedan" -> "Sedan";
            case "hatchback" -> "Hatchback";
            case "pickup" -> "Pickup";
            case "hybrid" -> "Hybrid";
            default -> "SUV";
        };
    }

    static List<String> categories() {
        return Arrays.asList("suv", "crossover", "sedan", "hatchback", "pickup", "hybrid");
    }

    static String slug(String input) {
        String s = safe(input).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return s.isBlank() ? "vehicle" : s;
    }

    static String formatTime(String stored) {
        try {
            return LocalDateTime.parse(stored, STORED).format(SHOW);
        } catch (Exception e) {
            return stored;
        }
    }

    static String normalizePriceLkr(String raw) {
        String digits = safe(raw).replaceAll("[^0-9]", "");
        return digits.isBlank() ? "0" : digits;
    }

    static String formatLkr(String raw) {
        String digits = normalizePriceLkr(raw);
        try {
            return "LKR " + String.format(Locale.US, "%,d", Long.parseLong(digits));
        } catch (Exception e) {
            return "LKR " + digits;
        }
    }

    static String defaultImage(String category) {
        return "/assets/img/listings/" + category + "-1.jpg";
    }

    static String seedData() {
        List<Vehicle> list = new ArrayList<>();
        String t = LocalDateTime.now().minusDays(2).format(STORED);

        list.add(new Vehicle("suv-1", "suv", "Land Rover Defender 110", "28900000", "2021", "48,000 km", "Diesel",
                "Automatic", "Iconic premium SUV with rugged styling, upscale presence, and strong enthusiast appeal.",
                "/assets/img/listings/suv-1.jpg", t, false));
        list.add(new Vehicle("suv-2", "suv", "Toyota Land Cruiser Prado TX", "19800000", "2020", "55,000 km", "Petrol",
                "Automatic",
                "Popular full-size SUV with premium road presence, family practicality, and proven resale strength.",
                "/assets/img/listings/suv-2.jpg", t, false));
        list.add(new Vehicle("suv-3", "suv", "Toyota Land Cruiser Prado TZ", "31500000", "2022", "31,000 km", "Diesel",
                "Automatic",
                "Premium seven-seat SUV with upscale comfort, commanding design, and trusted long-distance capability.",
                "/assets/img/listings/suv-3.jpg", t, false));
        list.add(new Vehicle("suv-4", "suv", "Toyota Land Cruiser Prado VX", "22500000", "2021", "44,000 km", "Petrol",
                "Automatic", "Well-kept premium SUV with refined comfort, road presence, and broad family appeal.",
                "/assets/img/listings/suv-4.jpg", t, false));
        list.add(new Vehicle("suv-5", "suv", "Land Rover Defender Adventure", "26800000", "2020", "62,000 km", "Diesel",
                "Automatic",
                "Adventure-ready SUV with signature styling, commanding ride height, and premium off-road character.",
                "/assets/img/listings/suv-1.jpg", t, false));
        list.add(new Vehicle("suv-6", "suv", "Toyota Land Cruiser Prado 2.7", "24200000", "2021", "40,000 km", "Petrol",
                "Automatic",
                "Comfortable premium SUV with dependable ownership appeal and practical everyday usability.",
                "/assets/img/listings/suv-2.jpg", t, false));
        list.add(new Vehicle("crossover-1", "crossover", "Jeep Renegade Limited", "17900000", "2022", "26,000 km",
                "Petrol", "Automatic",
                "Compact crossover with bold styling, upright stance, and strong urban personality.",
                "/assets/img/listings/crossover-1.jpg", t, false));
        list.add(new Vehicle("crossover-2", "crossover", "Ford Explorer XLT", "16800000", "2021", "39,000 km", "Petrol",
                "Automatic",
                "Spacious crossover-style SUV with smooth highway comfort and strong family-road-trip appeal.",
                "/assets/img/listings/crossover-2.jpg", t, false));
        list.add(new Vehicle("crossover-3", "crossover", "Range Rover Evoque", "20400000", "2023", "19,000 km",
                "Petrol", "Automatic",
                "Premium compact crossover with upscale styling, elevated seating, and standout showroom appeal.",
                "/assets/img/listings/crossover-3.jpg", t, false));
        list.add(new Vehicle("crossover-4", "crossover", "Land Rover Defender 90", "15700000", "2020", "47,000 km",
                "Diesel", "Automatic",
                "Distinctive premium crossover-SUV with rugged looks and excellent lifestyle appeal.",
                "/assets/img/listings/crossover-4.jpg", t, false));
        list.add(new Vehicle("crossover-5", "crossover", "Jeep Renegade Longitude", "14900000", "2021", "35,000 km",
                "Petrol", "Automatic",
                "Stylish compact crossover with practical size, easy maneuverability, and premium visual character.",
                "/assets/img/listings/crossover-1.jpg", t, false));
        list.add(new Vehicle("crossover-6", "crossover", "Ford Explorer Limited", "17100000", "2022", "28,000 km",
                "Petrol", "Automatic",
                "Comfortable family-focused crossover with upscale proportions and confident road presence.",
                "/assets/img/listings/crossover-2.jpg", t, false));
        list.add(new Vehicle("sedan-1", "sedan", "BMW 320i", "15200000", "2021", "41,000 km", "Petrol", "Automatic",
                "Premium sports sedan with balanced performance, executive styling, and everyday usability.",
                "/assets/img/listings/sedan-1.jpg", t, false));
        list.add(new Vehicle("sedan-2", "sedan", "Mercedes-Benz CLA 180", "18400000", "2022", "28,000 km", "Petrol",
                "Automatic", "Sleek premium sedan with modern styling, refined comfort, and strong visual appeal.",
                "/assets/img/listings/sedan-2.jpg", t, false));
        list.add(new Vehicle("sedan-3", "sedan", "Mitsubishi Lancer Evolution VIII", "24700000", "2020", "52,000 km",
                "Petrol", "Automatic",
                "Legendary turbo sports sedan with aggressive styling and collector-style enthusiast appeal.",
                "/assets/img/listings/sport-sedan-1.jpg", t, false));
        list.add(new Vehicle("sedan-4", "sedan", "Mitsubishi Lancer Evolution IX MR", "24600000", "2021", "33,000 km",
                "Petrol", "Automatic",
                "Well-kept performance sedan with unmistakable styling and strong tuner-market desirability.",
                "/assets/img/listings/sport-sedan-2.jpg", t, false));
        list.add(new Vehicle("sedan-5", "sedan", "Subaru WRX STI", "23200000", "2021", "37,000 km", "Petrol", "Manual",
                "Iconic all-wheel-drive sports sedan with rally heritage and sharp road presence.",
                "/assets/img/listings/sport-sedan-3.jpg", t, false));
        list.add(new Vehicle("sedan-6", "sedan", "Mitsubishi Lancer Evolution X", "22900000", "2020", "49,000 km",
                "Petrol", "Automatic",
                "Modern Evo styling with high-impact visual appeal and strong enthusiast demand.",
                "/assets/img/listings/sport-sedan-4.jpg", t, false));
        list.add(new Vehicle("sedan-7", "sedan", "Mitsubishi Lancer Evolution IX", "21400000", "2020", "46,000 km",
                "Petrol", "Manual",
                "Driver-focused performance sedan with bold front-end styling and unmistakable Evo character.",
                "/assets/img/listings/sport-sedan-5.jpg", t, false));
        list.add(new Vehicle("sedan-8", "sedan", "Subaru WRX STI Rally Blue", "22100000", "2021", "36,000 km", "Petrol",
                "Manual", "Rally-inspired sports sedan with iconic blue paintwork and strong enthusiast recognition.",
                "/assets/img/listings/sport-sedan-6.jpg", t, false));
        list.add(new Vehicle("sedan-9", "sedan", "Mitsubishi Lancer Evolution VIII RS", "23800000", "2021", "29,000 km",
                "Petrol", "Manual",
                "Track-ready Evo look with aggressive stance, premium finish, and standout showroom impact.",
                "/assets/img/listings/sport-sedan-7.jpg", t, false));
        list.add(new Vehicle("sedan-10", "sedan", "Mitsubishi Lancer Evolution VII", "20500000", "2020", "43,000 km",
                "Petrol", "Manual", "Classic turbo sports sedan with signature styling and lasting performance appeal.",
                "/assets/img/listings/sport-sedan-8.jpg", t, false));
        list.add(new Vehicle("sedan-11", "sedan", "Mitsubishi Lancer Evolution VIII Turbo", "22600000", "2021",
                "31,000 km", "Petrol", "Manual",
                "High-impact sports sedan with a planted stance and strong enthusiast-market desirability.",
                "/assets/img/listings/sport-sedan-9.jpg", t, false));
        list.add(new Vehicle("sedan-12", "sedan", "Subaru WRX STI Sedan", "21900000", "2022", "24,000 km", "Petrol",
                "Manual",
                "Performance sedan with rally-bred styling, practical four-door usability, and broad tuner appeal.",
                "/assets/img/listings/sport-sedan-10.jpg", t, false));
        list.add(new Vehicle("sedan-13", "sedan", "Subaru Impreza WRX STI", "21700000", "2021", "27,000 km", "Petrol",
                "Manual",
                "Aggressive performance sedan with signature front-end styling and premium enthusiast appeal.",
                "/assets/img/listings/sport-sedan-11.jpg", t, false));
        list.add(new Vehicle("sedan-14", "sedan", "Mitsubishi Lancer Evolution VIII Forest Edition", "22400000", "2020",
                "34,000 km", "Petrol", "Manual",
                "Distinctive Evo styling presented in an eye-catching outdoor setting with strong visual presence.",
                "/assets/img/listings/sport-sedan-12.jpg", t, false));
        list.add(new Vehicle("sedan-15", "sedan", "Subaru WRX STI Autumn Edition", "22800000", "2022", "21,000 km",
                "Petrol", "Manual",
                "Low-slung performance sedan with iconic Subaru styling and striking showroom character.",
                "/assets/img/listings/sport-sedan-13.jpg", t, false));
        list.add(new Vehicle("hatchback-1", "hatchback", "Suzuki Wagon R Stingray", "13600000", "2020", "46,000 km",
                "Hybrid", "Automatic",
                "Practical city hatchback with upright visibility, compact size, and strong daily usability.",
                "/assets/img/listings/hatchback-1.jpg", t, false));
        list.add(new Vehicle("hatchback-2", "hatchback", "Classic Mini Cooper", "10500000", "2022", "21,000 km",
                "Petrol", "Automatic", "Iconic small hatch with timeless styling and distinctive enthusiast appeal.",
                "/assets/img/listings/hatchback-2.jpg", t, false));
        list.add(new Vehicle("hatchback-3", "hatchback", "Cupra Born e-Boost", "14800000", "2021", "36,000 km",
                "Electric", "Automatic",
                "Modern performance hatch with sporty styling, premium finish, and standout road presence.",
                "/assets/img/listings/hatchback-3.jpg", t, false));
        list.add(new Vehicle("hatchback-4", "hatchback", "Volkswagen Golf TSI", "11600000", "2021", "29,000 km",
                "Petrol", "Automatic",
                "Refined hatchback with clean styling, premium road manners, and practical everyday comfort.",
                "/assets/img/listings/hatchback-4.jpg", t, false));
        list.add(new Vehicle("hatchback-5", "hatchback", "Suzuki Wagon R FX Hybrid", "9800000", "2020", "38,000 km",
                "Hybrid", "Automatic",
                "Efficient compact hatch with smart running costs and excellent city-car practicality.",
                "/assets/img/listings/hatchback-1.jpg", t, false));
        list.add(new Vehicle("hatchback-6", "hatchback", "Morris Mini", "16200000", "2021", "24,000 km", "Petrol",
                "Automatic", "Classic compact hatchback with vintage character and unique collector-style charm.",
                "/assets/img/listings/hatchback-2.jpg", t, false));
        list.add(new Vehicle("pickup-1", "pickup", "Ram 1500 Rebel", "23100000", "2021", "46,000 km", "Diesel",
                "Automatic",
                "Premium full-size pickup with a bold front end, upscale road presence, and strong lifestyle appeal.",
                "/assets/img/listings/pickup-1.jpg", t, false));
        list.add(new Vehicle("pickup-2", "pickup", "Ford F-150 Raptor Night Edition", "27900000", "2022", "42,000 km",
                "Petrol", "Automatic",
                "High-impact performance pickup with signature Raptor styling, premium detailing, and standout showroom appeal.",
                "/assets/img/listings/pickup-2.jpg", t, false));
        list.add(new Vehicle("pickup-3", "pickup", "Ram 2500 Laramie", "24500000", "2020", "53,000 km", "Diesel",
                "Automatic",
                "Heavy-duty premium pickup with commanding proportions, rugged capability, and refined long-distance comfort.",
                "/assets/img/listings/pickup-3.jpg", t, false));
        list.add(new Vehicle("pickup-4", "pickup", "Ford Ranger Raptor Urban", "22400000", "2021", "49,000 km",
                "Diesel", "Automatic",
                "Well-kept off-road pickup with aggressive stance, muscular styling, and excellent everyday versatility.",
                "/assets/img/listings/pickup-4.jpg", t, false));
        list.add(new Vehicle("pickup-5", "pickup", "Chevrolet C10 Classic", "15800000", "1979", "71,000 km", "Petrol",
                "Automatic", "Classic American pickup with timeless square-body styling and strong collector appeal.",
                "/assets/img/listings/pickup-5.jpg", t, false));
        list.add(new Vehicle("pickup-6", "pickup", "Toyota Hilux GR Sport", "23600000", "2022", "38,000 km", "Diesel",
                "Automatic",
                "Sporty lifestyle pickup with tough styling, dependable utility, and strong market demand.",
                "/assets/img/listings/pickup-6.jpg", t, false));
        list.add(new Vehicle("hybrid-1", "hybrid", "Suzuki Wagon R Stingray Hybrid", "10200000", "2021", "34,000 km",
                "Hybrid", "Automatic",
                "Compact hybrid hatch with smart urban proportions, low running costs, and easy day-to-day usability.",
                "/assets/img/listings/hybrid-1.jpg", t, false));
        list.add(new Vehicle("hybrid-2", "hybrid", "Suzuki Wagon R FX Hybrid", "9800000", "2020", "39,000 km", "Hybrid",
                "Automatic",
                "Efficient city hatchback with practical cabin space, light controls, and dependable fuel-saving appeal.",
                "/assets/img/listings/hybrid-2.jpg", t, false));
        list.add(new Vehicle("hybrid-3", "hybrid", "Toyota Prius S Touring", "12900000", "2021", "41,000 km", "Hybrid",
                "Automatic",
                "Popular Prius hybrid with proven fuel savings, smooth commuting comfort, and strong resale value.",
                "/assets/img/listings/hybrid-3.jpg", t, false));
        list.add(new Vehicle("hybrid-4", "hybrid", "Ford Escape Hybrid Titanium", "16800000", "2022", "28,000 km",
                "Hybrid", "Automatic",
                "Modern hybrid SUV with practical family space, efficient performance, and confident road presence.",
                "/assets/img/listings/hybrid-4.jpg", t, false));
        list.add(new Vehicle("hybrid-5", "hybrid", "Range Rover Evoque P300e", "21400000", "2022", "26,000 km",
                "Plug-in Hybrid", "Automatic",
                "Premium plug-in hybrid SUV with upscale styling, elevated comfort, and strong showroom appeal.",
                "/assets/img/listings/hybrid-5.jpg", t, false));
        list.add(new Vehicle("hybrid-6", "hybrid", "BMW 330e M Sport", "18600000", "2021", "33,000 km",
                "Plug-in Hybrid", "Automatic",
                "Sporty plug-in hybrid sedan with premium design, refined cabin feel, and efficient executive performance.",
                "/assets/img/listings/hybrid-6.jpg", t, false));

        list.add(new Vehicle("sedan-16", "sedan", "Audi A6 Black Edition", "17800000", "2020", "58,000 km", "Petrol",
                "Automatic",
                "Executive sedan with a clean black finish, premium road presence, and refined daily-driving comfort.",
                "/assets/img/listings/fresh-1.jpg", t, false));
        list.add(new Vehicle("sedan-17", "sedan", "Volkswagen Passat TSI", "15400000", "2021", "43,000 km", "Petrol",
                "Automatic",
                "Well-balanced family sedan with elegant styling, strong cabin comfort, and sensible ownership appeal.",
                "/assets/img/listings/fresh-2.jpg", t, false));
        list.add(new Vehicle("sedan-18", "sedan", "Audi A4 Quattro", "16900000", "2021", "37,000 km", "Petrol",
                "Automatic",
                "Smart executive sedan with understated styling, premium finish, and confident all-round usability.",
                "/assets/img/listings/fresh-3.jpg", t, false));
        list.add(new Vehicle("sedan-19", "sedan", "BMW 330i M Sport", "19600000", "2022", "29,000 km", "Petrol",
                "Automatic",
                "Sporty premium sedan with sharp design, balanced performance, and standout showroom appeal.",
                "/assets/img/listings/fresh-4.jpg", t, false));
        list.add(new Vehicle("sedan-20", "sedan", "BMW E30 Classic", "14200000", "1990", "84,000 km", "Petrol",
                "Manual",
                "Classic BMW sedan with enthusiast charm, timeless proportions, and collector-friendly road presence.",
                "/assets/img/listings/fresh-5.jpg", t, false));
        list.add(new Vehicle("sedan-21", "sedan", "Toyota Corolla Sport", "13600000", "2022", "24,000 km", "Petrol",
                "Automatic",
                "Modern compact sedan with clean styling, excellent practicality, and strong everyday value.",
                "/assets/img/listings/fresh-6.jpg", t, false));
        list.add(new Vehicle("hatchback-7", "hatchback", "MINI Cooper S", "14800000", "2021", "26,000 km", "Petrol",
                "Automatic",
                "Premium hatchback with playful styling, premium cabin feel, and strong city-driving character.",
                "/assets/img/listings/fresh-7.jpg", t, false));
        list.add(new Vehicle("sedan-22", "sedan", "BMW i4 M50", "23800000", "2023", "18,000 km", "Electric",
                "Automatic",
                "Modern electric performance sedan with striking front-end styling and premium technology appeal.",
                "/assets/img/listings/fresh-8.jpg", t, false));
        list.add(new Vehicle("sedan-23", "sedan", "Mercedes-Benz A 200 Sedan", "17100000", "2022", "23,000 km",
                "Petrol", "Automatic",
                "Compact Mercedes sedan with upscale design, premium badge appeal, and smooth everyday comfort.",
                "/assets/img/listings/fresh-9.jpg", t, false));
        list.add(new Vehicle("crossover-7", "crossover", "MINI Countryman", "15900000", "2021", "34,000 km", "Petrol",
                "Automatic",
                "Distinctive compact crossover with elevated stance, premium feel, and easy urban practicality.",
                "/assets/img/listings/fresh-10.jpg", t, false));
        list.add(new Vehicle("hatchback-8", "hatchback", "MINI Cooper White Edition", "14500000", "2020", "39,000 km",
                "Petrol", "Automatic",
                "Stylish premium hatch with iconic MINI character, tidy proportions, and strong visual appeal.",
                "/assets/img/listings/fresh-11.jpg", t, false));
        list.add(new Vehicle("sedan-24", "sedan", "BMW 330e Redline", "18800000", "2021", "33,000 km", "Petrol",
                "Automatic",
                "Eye-catching BMW sedan with sporty detailing, premium finish, and confident road presence.",
                "/assets/img/listings/fresh-12.jpg", t, false));
        list.add(new Vehicle("sedan-25", "sedan", "BMW 328i Alpine White", "17600000", "2020", "46,000 km", "Petrol",
                "Automatic", "Clean and elegant premium sedan with crisp styling and broad buyer-friendly appeal.",
                "/assets/img/listings/fresh-13.jpg", t, false));
        list.add(new Vehicle("sedan-26", "sedan", "BMW 335i Shadowline", "18400000", "2021", "41,000 km", "Petrol",
                "Automatic",
                "Sharp black sedan with premium styling, composed performance, and executive-road presence.",
                "/assets/img/listings/fresh-14.jpg", t, false));
        list.add(new Vehicle("hatchback-9", "hatchback", "Volkswagen Golf GTI Classic", "13900000", "2020", "52,000 km",
                "Petrol", "Automatic",
                "Classic hot hatch styling with strong enthusiast appeal and practical everyday usability.",
                "/assets/img/listings/fresh-15.jpg", t, false));

        StringBuilder sb = new StringBuilder();
        for (Vehicle v : list) {
            sb.append(v.toTsv()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    static String profileMessage(Map<String, String> query) {
        return safe(query.getOrDefault("msg", ""));
    }

    static String requestMessage(Map<String, String> query) {
        return safe(query.getOrDefault("msg", ""));
    }

    static String reviewMessage(Map<String, String> query) {
        return safe(query.getOrDefault("msg", ""));
    }

    static String premiumProfilePage(HttpExchange exchange, boolean buyerProfile) throws IOException {
        AppUser user = currentUser(exchange);
        String html = Files.readString(
                TEMPLATES_DIR.resolve(buyerProfile ? "profile/buyer-profile.html" : "profile/premium-profile.html"),
                StandardCharsets.UTF_8);
        if (user != null) {
            html = html.replace("Kasun Perera", esc(user.getUsername()));
            html = html.replace("kasun.perera@gmail.com", esc(user.getEmail()));
            html = html.replace("+94 77 123 4567", esc(digitsOnly(user.getPhone())));
            html = html.replace("0771234567", esc(digitsOnly(user.getPhone())));
            html = html.replace("kasun_automart", esc(user.getUsername()));
            html = html.replace("@kasun_automart", "@" + esc(user.getUsername()));
            html = html.replace("<div class=\"tb-av\" id=\"profileTopAvatar\">KP</div>",
                    "<div class=\"tb-av\" id=\"profileTopAvatar\">" + esc(initials(user.getUsername())) + "</div>");
            html = html.replace("<div class=\"avatar-inner\" id=\"profileHeroAvatar\">K</div>",
                    "<div class=\"avatar-inner\" id=\"profileHeroAvatar\">" + esc(initials(user.getUsername()))
                            + "</div>");
        }
        return html;
    }

    static String premiumProfilePage(HttpExchange exchange) throws IOException {
        return premiumProfilePage(exchange, false);
    }

    static String profileContent(HttpExchange exchange, String message) throws IOException {
        AppUser user = currentUser(exchange);
        if (user == null) {
            return "<section class='container section'><div class='panel'><h1>No active user</h1></div></section>";
        }
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        String alertClass = lowerMessage.contains("updated") || lowerMessage.contains("saved") ? "alert success-alert"
                : "alert";
        String alert = message.isBlank() ? "" : "<div class='" + alertClass + "'>" + esc(message) + "</div>";
        String roleNote = user.roleMessage();
        String initials = esc(initials(user.getUsername()));
        String role = esc(user.getRole());
        String heroTitle = user instanceof BuyerUser ? "Buyer Profile Studio"
                : user instanceof SellerUser ? "Seller Profile Studio" : "Account Profile Studio";
        String heroCopy = user instanceof BuyerUser
                ? "Upload a profile photo, personalize your buying preferences, and make your Auto Mart identity feel more premium."
                : "Refresh your account identity, contact channels, and marketplace preferences with a more polished profile workspace.";
        return "<section class='container section profile-page'><p class='eyebrow'>USER MANAGEMENT</p><h1>Profile Management</h1><p class='lead profile-page-lead'>"
                + esc(roleNote) + "</p>" + alert
                + "<div class='profile-shell'>"
                + "<aside class='panel profile-sidebar'>"
                + "<div class='profile-hero-card'>"
                + "<div class='profile-hero-glow'></div>"
                + "<div class='profile-avatar-stack'>"
                + "<div class='profile-avatar-upload'>"
                + "<label class='profile-avatar-label' for='profile-picture-input'>"
                + "<span class='profile-avatar-frame'><img id='profile-avatar-image' class='profile-avatar-image' alt='Profile preview'><span id='profile-avatar-fallback' class='profile-avatar-fallback'>"
                + initials + "</span><span class='profile-avatar-orbit'></span></span>"
                + "<span id='profile-avatar-edit-text' class='profile-avatar-edit'>Add profile photo</span>"
                + "</label>"
                + "<div class='profile-avatar-actions'><label class='profile-avatar-action profile-avatar-action-primary' for='profile-picture-input'>Choose photo</label><button id='profile-remove-photo' class='profile-avatar-action profile-avatar-action-secondary' type='button'>Remove photo</button></div>"
                + "<input id='profile-picture-input' class='profile-picture-input' type='file' accept='.jpg, .jpeg, .png'>"
                + "<p class='profile-avatar-hint'>PNG, JPG, or WEBP work best. Your photo preview syncs with the top profile badge instantly.</p>"
                + "</div>"
                + "<div><span class='profile-role-pill'>" + role + " account</span><h2>" + esc(heroTitle) + "</h2><p>"
                + esc(heroCopy) + "</p></div>"
                + "</div>"
                + "<div class='profile-completion-card'>"
                + "<div class='profile-completion-ring' data-completion='78'><svg viewBox='0 0 120 120' aria-hidden='true'><circle cx='60' cy='60' r='48'></circle><circle class='progress' cx='60' cy='60' r='48'></circle></svg><strong id='profile-completion-value'>78%</strong></div>"
                + "<div><strong>Profile strength</strong><span>Complete more details to build trust faster with buyers, sellers, and the Auto Mart team.</span></div>"
                + "</div>"
                + "<div class='profile-quick-stats'>"
                + "<article class='profile-mini-stat'><span>Role</span><strong>" + role + "</strong></article>"
                + "<article class='profile-mini-stat'><span>Preferences</span><strong>6+</strong></article>"
                + "<article class='profile-mini-stat'><span>Status</span><strong>Active</strong></article>"
                + "</div>"
                + "<article class='profile-preferences-panel'>"
                + "<span class='profile-section-badge'>Quick preferences</span>"
                + "<div class='profile-chip-group'>"
                + "<button class='profile-chip is-active' type='button' data-chip='sedans'>Sedans</button>"
                + "<button class='profile-chip' type='button' data-chip='suvs'>SUVs</button>"
                + "<button class='profile-chip' type='button' data-chip='hybrids'>Hybrids</button>"
                + "<button class='profile-chip' type='button' data-chip='low-mileage'>Low mileage</button>"
                + "<button class='profile-chip' type='button' data-chip='verified'>Verified listings</button>"
                + "</div>"
                + "</article>"
                + "</div>"
                + "</aside>"
                + "<div class='profile-main-stack'>"
                + "<form id='buyer-profile-form' class='panel sell-form profile-enhanced-form' method='post' action='/profile'>"
                + "<div class='profile-section-head'><div><span class='form-section-kicker'>Identity &amp; contact</span><h3>Main account details</h3></div><span class='profile-helper-text'>Core fields save to your account. Advanced options stay in your browser for this device.</span></div>"
                + "<div class='profile-form-grid profile-form-grid-two'>"
                + fieldWithValue("Username", "username", "username", user.getUsername())
                + fieldWithValue("Email", "email", "Enter your Gmail address", user.getEmail())
                + fieldWithValue("Phone", "phone", "0771234567", digitsOnly(user.getPhone()))
                + "<label class='profile-password-label'>Password<div class='profile-password-shell'><input id='profile-password' type='password' name='password' placeholder='Enter new password' value='"
                + esc(user.getPassword())
                + "' minlength='8' autocomplete='new-password' pattern='^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$' title='Use at least 8 characters with uppercase, lowercase, and a number.'><button class='profile-password-toggle' type='button' data-password-toggle='profile-password' aria-label='Show password'>Show</button></div><small class='profile-password-help'>Use 8+ characters with uppercase, lowercase, and a number.</small><span id='profile-password-strength' class='profile-password-strength' data-strength='weak'><span></span></span><small id='profile-password-strength-copy' class='profile-password-strength-copy'>Security level: weak</small></label>"
                + "<label class='profile-password-label'>Confirm password<div class='profile-password-shell'><input id='profile-password-confirm' type='password' name='confirmPassword' placeholder='Re-enter password to confirm' minlength='8' autocomplete='new-password'><button class='profile-password-toggle' type='button' data-password-toggle='profile-password-confirm' aria-label='Show confirm password'>Show</button></div><small id='profile-password-match' class='profile-password-help'>Re-enter the same password before saving.</small></label>"
                + "<label>Display name<input type='text' name='displayName' placeholder='How your name appears publicly' value='"
                + esc(user.getUsername()) + "'></label>"
                + "<label>Location<input type='text' name='location' placeholder='Colombo, Sri Lanka'></label>"
                + "</div>"
                + "<label class='profile-textarea-label'>Bio<textarea name='bio' rows='5' placeholder='Share your preferences, response style, and what kind of cars you are looking for.'></textarea></label>"
                + "<div class='profile-section-head'><div><span class='form-section-kicker'>Buyer setup</span><h3>Marketplace preferences</h3></div><span class='profile-helper-text'>These options improve the buyer experience and add motion-rich UI interactions.</span></div>"
                + "<div class='profile-form-grid profile-form-grid-two'>"
                + "<label>Preferred body type<select name='bodyType'><option value=''>Choose body type</option><option>Sedan</option><option>SUV</option><option>Hatchback</option><option>Crossover</option><option>Coupe</option><option>Van</option></select></label>"
                + "<label>Budget range<select name='budgetRange'><option value=''>Choose budget</option><option>Under LKR 5M</option><option>LKR 5M - 10M</option><option>LKR 10M - 15M</option><option>LKR 15M - 25M</option><option>Above LKR 25M</option></select></label>"
                + "<label>Fuel preference<select name='fuelPreference'><option value=''>Choose fuel</option><option>Petrol</option><option>Diesel</option><option>Hybrid</option><option>Electric</option></select></label>"
                + "<label>Transmission<select name='transmissionPreference'><option value=''>Choose transmission</option><option>Automatic</option><option>Manual</option><option>No preference</option></select></label>"
                + "<label>Preferred contact<select name='preferredContact'><option value=''>Choose method</option><option>Phone</option><option>WhatsApp</option><option>Email</option><option>In-app chat</option></select></label>"
                + "<label>Inspection city<input type='text' name='inspectionCity' placeholder='Colombo / Kandy / Galle'></label>"
                + "</div>"
                + "<div class='profile-toggle-grid'>"
                + "<label class='profile-toggle-card'><input type='checkbox' name='alertsEnabled' checked><span class='profile-toggle-ui'></span><span class='profile-toggle-copy'><strong>Instant alerts</strong><small>Get notified when matching vehicles are listed.</small></span></label>"
                + "<label class='profile-toggle-card'><input type='checkbox' name='inspectionReminders' checked><span class='profile-toggle-ui'></span><span class='profile-toggle-copy'><strong>Inspection reminders</strong><small>Receive prompts for scheduled visits and follow ups.</small></span></label>"
                + "<label class='profile-toggle-card'><input type='checkbox' name='showBuyerBadge' checked><span class='profile-toggle-ui'></span><span class='profile-toggle-copy'><strong>Verified buyer badge</strong><small>Highlight a stronger and more trusted account presence.</small></span></label>"
                + "<label class='profile-toggle-card'><input type='checkbox' name='darkEffects' checked><span class='profile-toggle-ui'></span><span class='profile-toggle-copy'><strong>Premium motion effects</strong><small>Enable tilt, glow, and spotlight visuals on this page.</small></span></label>"
                + "</div>"
                + "<div class='profile-action-row'><button class='btn btn-primary profile-save-btn' type='submit'>Save Profile</button><a class='btn btn-secondary' href='/profile/details'>View User Details</a><a class='btn btn-secondary' href='/profile/export'>Download User Text File</a><button class='btn btn-secondary' id='profile-reset-local' type='button'>Reset local extras</button></div>"
                + "</form>"
                + "<div class='profile-detail-grid'>"
                + "<article class='panel profile-effect-card profile-spotlight-card'><div class='profile-spotlight-copy'><span>Buyer spotlight</span><strong>Build a profile that feels premium and trustworthy.</strong><p>Add a photo, personalize your preferences, and keep your request flow polished with a richer account page.</p></div><div class='profile-floating-orbs' aria-hidden='true'><span></span><span></span><span></span></div></article>"
                + "<article class='panel profile-effect-card'><div class='profile-section-head'><div><span class='form-section-kicker'>Account extras</span><h3>More options</h3></div></div><div class='profile-form-grid'>"
                + "<label>Lifestyle tag<select name='lifestyleTag'><option value=''>Select a style</option><option>City commuter</option><option>Family focused</option><option>Performance enthusiast</option><option>Luxury seeker</option><option>Weekend explorer</option></select></label>"
                + "<label>Favorite brand<input type='text' name='favoriteBrand' placeholder='BMW, Toyota, Mercedes-Benz...'></label>"
                + "<label>Monthly finance target<input type='text' name='financeTarget' placeholder='LKR 150,000 per month'></label>"
                + "<label class='profile-textarea-label'>Wishlist notes<textarea name='wishlistNotes' rows='4' placeholder='Leather seats, panoramic roof, adaptive cruise control, low mileage, etc.'></textarea></label>"
                + "</div></article>"
                + "</div>"
                + "<article class='panel profile-danger-zone'><div class='profile-section-head'><div><span class='form-section-kicker'>Danger zone</span><h3>Delete account</h3></div><span class='profile-helper-text'>This removes your account access from the marketplace.</span></div><p>Delete the current account only if you are sure. This action removes your access to requests, reviews, and your profile dashboard.</p><form method='post' action='/profile/delete' data-delete-confirm=\"Delete this account?\"><button class='btn btn-danger danger' type='submit'>Delete Account</button></form></article>"
                + "</div>"
                + "</div>"
                + "</section>";
    }

    static String requestsContent(HttpExchange exchange, String message) throws IOException {
        List<Vehicle> vehicles = loadVehicles();
        List<PurchaseRequest> requests = loadRequests();
        StringBuilder vehicleOptions = new StringBuilder();
        for (Vehicle vehicle : vehicles.subList(0, Math.min(20, vehicles.size()))) {
            vehicleOptions.append("<option value='").append(esc(vehicle.id)).append("'>").append(esc(vehicle.title))
                    .append("</option>");
        }

        StringBuilder rows = new StringBuilder();
        for (PurchaseRequest request : requests) {
            rows.append("<tr><td>").append(esc(request.id)).append("</td><td>").append(esc(request.vehicleTitle))
                    .append("</td><td>").append(esc(request.buyerUsername)).append("</td><td>")
                    .append(esc(formatTime(request.createdAt))).append("</td><td>").append(esc(request.status))
                    .append("</td><td>")
                    .append("<form method='post' action='/requests/action' class='inline-form'>")

                    .append(hiddenField("id", request.id))
                    .append("<select name='status'><option>Pending</option><option>Approved</option><option>Rejected</option></select>")
                    .append("<button class='btn btn-secondary' type='submit' name='action' value='update'>Save</button>")
                    .append("<button class='btn btn-danger' type='submit' name='action' value='delete' data-delete-button data-delete-message='Delete this request permanently?'>Delete</button></form></td></tr>");
        }
        if (rows.length() == 0)
            rows.append("<tr><td colspan='5'>No requests yet.</td></tr>");
        String alert = message.isBlank() ? "" : "<div class='alert'>" + esc(message) + "</div>";
        return "<section class='container section'><p class='eyebrow'>BUYING & REQUEST MANAGEMENT</p><h1>Purchase Requests</h1><p class='lead'>Every request receives a unique ID and LocalDateTime timestamp for transaction tracking.</p>"
                + alert
                + "<div class='panel'><form class='sell-form' method='post' action='/requests'>"

                + "<label>Vehicle<select name='vehicleId'>" + vehicleOptions + "</select></label>"
                + field("Phone number", "contactPhone", "Enter 10-digit mobile number")
                + textareaField("Request note", "note", "I would like to inspect this car this weekend.")
                + "<button class='btn btn-primary' type='submit'>Send Request</button></form></div>"
                + "<div class='panel'><table><thead><tr><th>ID</th><th>Vehicle</th><th>Buyer</th><th>Created</th><th>Status</th><th>Actions</th></tr></thead><tbody>"
                + rows + "</tbody></table></div></section>";
    }

    static String reviewsContent(HttpExchange exchange, String message) throws IOException {
        List<Vehicle> vehicles = loadVehicles();
        List<ReviewEntry> reviews = loadReviews();
        AppUser user = currentUser(exchange);
        StringBuilder vehicleOptions = new StringBuilder();
        for (Vehicle vehicle : vehicles.subList(0, Math.min(20, vehicles.size()))) {
            vehicleOptions.append("<option value='").append(esc(vehicle.id)).append("'>").append(esc(vehicle.title))
                    .append("</option>");
        }

        double averageRating = reviews.stream().mapToInt(r -> parseIntSafe(r.rating, 0)).average().orElse(0);
        long verifiedCount = reviews.stream().filter(r -> "VerifiedReview".equalsIgnoreCase(r.type)).count();
        long publicCount = reviews.stream().filter(r -> "PublicReview".equalsIgnoreCase(r.type)).count();
        ReviewEntry latestReview = reviews.stream().max(Comparator.comparing(r -> safe(r.createdAt))).orElse(null);
        String topVehicle = reviews.stream()
                .collect(Collectors.groupingBy(r -> r.vehicleTitle, LinkedHashMap::new,
                        Collectors.averagingInt(r -> parseIntSafe(r.rating, 0))))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("No ratings yet");

        StringBuilder distribution = new StringBuilder();
        for (int stars = 5; stars >= 1; stars--) {
            final int starValue = stars;
            long count = reviews.stream().filter(r -> parseIntSafe(r.rating, 0) == starValue).count();
            int percent = reviews.isEmpty() ? 0 : (int) Math.round((count * 100.0) / reviews.size());
            distribution.append(reviewDistributionRow(starValue, count, percent));
        }

        StringBuilder cards = new StringBuilder();
        for (ReviewEntry review : reviews.stream()
                .sorted(Comparator.comparing((ReviewEntry r) -> safe(r.createdAt)).reversed()).toList()) {
            cards.append(review.toString());

        }
        if (cards.length() == 0)
            cards.append(
                    "<article class='panel review-empty-state'><h3>No reviews yet</h3><p>Be the first buyer to share your experience and help build marketplace trust.</p></article>");

        String alert = message.isBlank() ? "" : "<div class='alert'>" + esc(message) + "</div>";
        String authHint = user == null
                ? "<div class='review-auth-note'>Sign in to submit a review and manage your feedback.</div>"
                : "";
        String formAction = user == null
                ? "<div class='review-locked'><h3>Review submission requires login</h3><p>Sign in as a buyer, seller, or admin to rate a vehicle and share your experience.</p><a class='btn btn-primary' href='/auth'>Sign In</a></div>"
                : "<form class='review-form-pro' method='post' action='/reviews'>"
                        + "<div class='review-form-grid'>"
                        + "<label><span>Vehicle</span><select name='vehicleId'>" + vehicleOptions + "</select></label>"
                        + "<label><span>Review type</span><select name='type'><option>VerifiedReview</option><option>PublicReview</option></select></label>"
                        + "</div>"
                        + "<div class='rating-picker-row'>"
                        + "<span class='rating-picker-label'>Rating</span>"
                        + reviewStarInputs()
                        + "</div>"
                        + textareaField("Comment", "comment", "Great communication and a clean vehicle.")
                        + "<div class='review-form-actions'><button class='btn btn-primary' type='submit'>Publish Review</button><span class='review-form-note'>Verified reviews earn more trust</span></div>"
                        + "</form>";

        String latest = latestReview == null ? "No review submitted yet"
                : esc(latestReview.vehicleTitle) + " • " + starIcons(parseIntSafe(latestReview.rating, 0)) + " • "
                        + esc(formatTime(latestReview.createdAt));

        return "<section class='container section review-page'>"
                + "<div class='review-hero premium-glass-card'>"
                + "<div class='review-hero-copy'><p class='eyebrow'>FEEDBACK & REVIEW SYSTEM</p><h1>Professional Seller and Vehicle Reviews</h1><p class='lead'>Rate verified listings with confidence, highlight seller professionalism, and help buyers make faster decisions with detailed real-world feedback.</p>"
                + alert + authHint + "</div>"
                + "<div class='review-hero-stats'>"

                + reviewKpi("Average Rating", String.format(Locale.US, "%.1f/5", averageRating),
                        starIcons((int) Math.round(averageRating)))
                + reviewKpi("Total Reviews", String.valueOf(reviews.size()), "Across all submitted feedback")
                + reviewKpi("Verified Reviews", String.valueOf(verifiedCount),
                        verifiedCount + " trusted purchase-based reviews")
                + reviewKpi("Top Rated Vehicle", esc(topVehicle), latest)

                + "</div></div>"
                + "<div class='review-layout'>"
                + "<div class='review-composer premium-glass-card'><div class='review-section-head'><h2>Write a review</h2><p>Submit a complete review with star rating, review type, and detailed buyer notes.</p></div>"
                + formAction + "</div>"
                + "<aside class='review-insights'>"

                + "<article class='premium-glass-card review-summary-card'><h3>Rating Breakdown</h3><div class='review-score-display'><strong>"
                + String.format(Locale.US, "%.1f", averageRating)
                + "</strong><span>Overall rating</span><div class='review-score-stars'>"
                + starIcons((int) Math.round(averageRating)) + "</div></div><div class='review-distribution'>"
                + distribution + "</div></article>"
                + "<article class='premium-glass-card review-summary-card'><h3>Marketplace Trust</h3><div class='review-mini-grid'>"

                + miniTrustStat("Verified", String.valueOf(verifiedCount))
                + miniTrustStat("Public", String.valueOf(publicCount))
                + miniTrustStat("Latest", latestReview == null ? "—" : esc(formatTime(latestReview.createdAt)))
                + miniTrustStat("Response", "Fast")
                + "</div></article>"
                + "</aside></div>"
                + "<div class='review-list-head'><div><p class='eyebrow'>COMMUNITY FEEDBACK</p><h2>Complete review feed</h2><p>Every review shows vehicle, reviewer, type, rating, time, and full comment.</p></div><div class='review-feed-badge'>"
                + reviews.size() + " Reviews</div></div>"
                + "<div class='review-card-grid'>" + cards + "</div>"

                + "</section>";
    }

    static String reviewDistributionRow(int stars, long count, int percent) {
        return "<div class='review-distribution-row'><span class='review-distribution-label'>" + stars
                + " star</span><div class='review-distribution-bar'><span style='width:" + percent
                + "%'></span></div><strong>" + count + "</strong></div>";
    }

    static String reviewKpi(String label, String value, String subtext) {
        return "<article class='review-kpi'><span>" + esc(label) + "</span><strong>" + value + "</strong><small>"
                + subtext + "</small></article>";
    }

    static String miniTrustStat(String label, String value) {
        return "<div class='review-mini-stat'><span>" + esc(label) + "</span><strong>" + esc(value) + "</strong></div>";
    }

    static String starIcons(int rating) {
        int safeRating = Math.max(0, Math.min(5, rating));
        StringBuilder out = new StringBuilder(
                "<span class='review-stars' aria-label='" + safeRating + " out of 5 stars'>");
        for (int i = 1; i <= 5; i++) {

            out.append("<span class='star").append(i <= safeRating ? " is-filled" : "").append("'>★</span>");
        }
        out.append("</span>");
        return out.toString();
    }

    static String reviewStarInputs() {
        StringBuilder out = new StringBuilder("<div class='star-rating-input'>");
        for (int i = 5; i >= 1; i--) {
            out.append("<input type='radio' id='rating-").append(i).append("' name='rating' value='").append(i)
                    .append("'").append(i == 5 ? " checked" : "").append(">");
            out.append("<label for='rating-").append(i).append("' title='").append(i).append(" stars'>★</label>");
        }
        out.append("</div>");
        return out.toString();
    }

    static String adminContent() throws IOException {
        List<AppUser> users = loadUsers();
        List<Vehicle> vehicles = loadVehicles();
        List<PurchaseRequest> requests = loadRequests();
        List<ReviewEntry> reviews = loadReviews();
        long adminCount = users.stream().filter(u -> "admin".equalsIgnoreCase(u.getRole())).count();
        long buyerCount = users.stream().filter(u -> "buyer".equalsIgnoreCase(u.getRole())).count();
        long sellerCount = users.stream().filter(u -> "seller".equalsIgnoreCase(u.getRole())).count();
        long pendingRequests = requests.stream().filter(r -> "Pending".equalsIgnoreCase(r.status)).count();
        long pendingReviews = reviews.size();
        StringBuilder userRows = new StringBuilder();
        for (AppUser user : users) {
            userRows.append(
                    "<tr><form method='post' action='/admin/users/update' class='admin-user-row-form'><td><strong>")
                    .append(
                            esc(user.getUsername()))
                    .append("</strong>").append(hiddenField("username", user.getUsername()))
                    .append("</td><td><span class='admin-role-chip role-")
                    .append(esc(user.getRole().toLowerCase(Locale.ROOT))).append("'>").append(esc(user.getRole()))
                    .append("</span></td><td><input class='admin-table-input js-email-lower' name='email' value='")
                    .append(esc(user.getEmail()))
                    .append("' type='email' pattern='^[a-z0-9._]+@gmail\\.com$' title='Use a valid Gmail address like example@gmail.com' required></td><td>")
                    .append("<input class='admin-table-input js-phone-digits' name='phone' value='")
                    .append(esc(digitsOnly(user.getPhone())))
                    .append("' inputmode='numeric' maxlength='10' pattern='[0-9]{7,10}' title='Digits only, maximum 10 digits' required></td><td>")
                    .append("<button class='btn btn-secondary admin-save-btn' type='submit'>Save Edit</button></td></form></tr>");

        }
        List<String> logs = Files.exists(LOGS_FILE) ? Files.readAllLines(LOGS_FILE, StandardCharsets.UTF_8) : List.of();
        StringBuilder logItems = new StringBuilder();
        for (int i = Math.max(0, logs.size() - 8); i < logs.size(); i++)
            logItems.append("<li>").append(esc(logs.get(i))).append("</li>");
        String adminMsg = "<script>const m=new URLSearchParams(location.search).get('msg'); if(m){document.write(\"<div class='alert success admin-toast'>\"+m.replace(/[<>]/g,'')+\"</div>\");}</script>";
        return "<section class='container section admin-overview admin-premium-page'>" + adminMsg
                + "<div class='admin-hero panel animated-card'><div><p class='eyebrow'>ADMIN DASHBOARD</p><h1>Marketplace Control Panel</h1><p class='lead'>Manage users, edit the user text file, validate contact details, review listings, and monitor AutoMart activity from one clean staff workspace.</p><div class='actions'><a class='btn btn-primary premium-glow-btn' href='/admin/users.txt'>Download User Text File</a><a class='btn btn-secondary' href='/'>View Website</a></div></div><div class='admin-hero-card'><span>Staff Session</span><strong>Protected</strong><small>Admin authorization required</small></div></div>"
                + "<div class='admin-metric-grid admin-premium-metrics'>"
                + adminMetric("Total Users", String.valueOf(users.size()))
                + adminMetric("Admins", String.valueOf(adminCount))
                + adminMetric("Buyers", String.valueOf(buyerCount))
                + adminMetric("Sellers", String.valueOf(sellerCount))
                + adminMetric("Listings", String.valueOf(vehicles.size()))
                + adminMetric("Requests", String.valueOf(pendingRequests))
                + "</div>"
                + "<div class='admin-feature-grid'>"
                + dashboardTile("User Text File", "Download and edit saved user contact data with validation.")
                + dashboardTile("Email Validation",
                        "Admin edits automatically support lowercase email and block invalid characters.")
                + dashboardTile("Phone Validation",
                        "Phone inputs accept digits only and stay below 10 digits.")
                + dashboardTile("Seller Verification", "Track seller status and marketplace trust signals.")
                + dashboardTile("Review Moderation", "Monitor feedback and remove inappropriate reviews.")
                + dashboardTile("Request Monitoring", "Follow buyer requests and suspicious activity.")
                + "</div>"
                + "<div class='panel admin-user-panel'><div class='admin-panel-head'><div><p class='eyebrow'>USER MANAGEMENT</p><h2>Edit User Text File</h2><p>Edit user email and phone numbers here. Emails save as lowercase, phones use digits only, and all changes are saved into <code>users.txt</code>.</p></div><a class='btn btn-primary' href='/admin/users.txt'>Download TXT</a></div><div class='admin-table-wrap'><table class='admin-user-table'><thead><tr><th>Username</th><th>Role</th><th>Email</th><th>Phone</th><th>Action</th></tr></thead><tbody>"
                + userRows + "</tbody></table></div></div>"
                + "<div class='grid cards admin-bottom-grid'><article class='panel'><h2>Activity Logs</h2><ul class='plain-list admin-log-list'>"
                + (logItems.length() == 0 ? "<li>No activity logged yet.</li>" : logItems.toString())
                + "</ul></article><article class='panel'><h2>Backup & Monitoring</h2><p>Data files are stored under <code>src/main/resources/data</code> for quick backup during demos.</p><div class='admin-chip-row'><span>users.txt</span><span>requests.txt</span><span>reviews.txt</span></div></article></div>"

                + "<script>document.querySelectorAll('.js-email-lower').forEach(i=>i.addEventListener('input',()=>{i.value=i.value.toLowerCase().replace(/[#%]/g,'')})); document.querySelectorAll('.js-phone-digits').forEach(i=>i.addEventListener('input',()=>{i.value=i.value.replace(/\\D/g,'').slice(0,10)}));</script>"
                + "</section>";
    }

    static String adminMetric(String label, String value) {
        return "<article class='admin-metric-card'><span>" + esc(label) + "</span><strong>" + esc(value)
                + "</strong></article>";
    }

    static void ensureAdmin(HttpExchange exchange) {
        AppUser user = currentUser(exchange);
        if (user == null || !user.isAdmin()) {
            throw new IllegalStateException("Admin access only. Sign in as admin / admin123.");
        }
    }

    static boolean isAdminUser(HttpExchange exchange) {
        AppUser user = currentUser(exchange);
        return user != null && user.isAdmin();
    }

    static boolean isValidSriLankanNic(String nic) {
        String clean = safe(nic).trim().toUpperCase(Locale.ROOT);
        return clean.matches("\\d{12}") || clean.matches("\\d{9}[VX]");
    }

    static String normalizeEmailAddress(String email) {
        return safe(email).trim().toLowerCase(Locale.ROOT);
    }

    static String validateEmailAddress(String email) {
        email = normalizeEmailAddress(email);
        if (email.isBlank())
            return "Enter your Gmail address.";
        if (email.contains(" "))
            return "Gmail address cannot contain spaces.";
        if (email.chars().filter(ch -> ch == '@').count() != 1)
            return "Gmail address must contain one @ symbol.";
        if (!email.endsWith("@gmail.com"))
            return "Email must be a Gmail address like example@gmail.com.";
        String usernamePart = email.substring(0, email.indexOf('@'));
        if (usernamePart.isBlank())
            return "Enter the username before @gmail.com.";
        if (!usernamePart.matches("[a-z0-9._]+"))
            return "Gmail username can only use letters, numbers, dots, and underscores.";
        if (!EMAIL_PATTERN.matcher(email).matches())
            return "Enter a valid Gmail address like example@gmail.com.";
        return null;
    }

    static String digitsOnly(String phone) {
        return safe(phone).replaceAll("\\D", "");
    }

    static String validatePhoneNumber(String phone) {
        String digits = digitsOnly(phone);
        if (digits.isBlank())
            return "Enter 10-digit mobile number.";
        if (!digits.equals(safe(phone).trim()))
            return "Mobile number can contain numbers only.";
        if (digits.length() != 10)
            return "Mobile number must contain exactly 10 digits.";
        return null;
    }

    static String validateSettingsContact(Map<String, String> form) {
        for (String key : List.of("teamEmail", "billingEmail")) {
            String value = safe(form.getOrDefault(key, "")).trim();
            if (!value.isBlank()) {
                String error = validateEmailAddress(value);
                if (error != null)
                    return error;
            }

        }
        for (String key : List.of("phone", "whatsappNumber")) {
            String value = safe(form.getOrDefault(key, "")).trim();
            if (!value.isBlank()) {
                String error = validatePhoneNumber(value);
                if (error != null)
                    return error;
            }

        }
        return null;
    }

    static String updateUserFromAdmin(Map<String, String> form) throws IOException {
        String username = safe(form.getOrDefault("username", "")).trim();
        String email = normalizeEmailAddress(form.getOrDefault("email", ""));
        String phone = safe(form.getOrDefault("phone", "")).trim();
        String emailError = validateEmailAddress(email);
        if (emailError != null)
            return emailError;
        String phoneError = validatePhoneNumber(phone);
        if (phoneError != null)
            return phoneError;
        List<AppUser> users = loadUsers();
        boolean updated = false;
        for (AppUser user : users) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                user.setEmail(email);
                user.setPhone(digitsOnly(phone));
                updated = true;
            }
        }
        if (!updated)
            return "User not found.";
        Files.write(USERS_FILE, users.stream().map(AppUser::toRecord).toList(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        logActivity("ADMIN_USER_UPDATED", "Admin edited contact details for " + username);

        return "User text file updated successfully.";
    }

    static AppUser currentUser(HttpExchange exchange) {
        String username = safe(SessionManager.currentUsername(exchange)).trim();
        if (username.isBlank()) {
            return null;
        }
        try {
            return findUser(username);
        } catch (IOException e) {
            return null;
        }
    }

    static void seedUsers() throws IOException {
        if (Files.exists(USERS_FILE) && Files.size(USERS_FILE) > 0)
            return;
        List<String> lines = List.of(

                new AdminUser("admin", "admin.automart@gmail.com", "0110000000", "admin123").toRecord(),
                new BuyerUser("buyer1", "buyer1@gmail.com", "0770000001", "buyer123").toRecord(),
                new SellerUser("seller1", "seller1@gmail.com", "0770000002", "seller123").toRecord());
        Files.write(USERS_FILE, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    static void seedRequests() throws IOException {
        if (!Files.exists(REQUESTS_FILE))
            Files.writeString(REQUESTS_FILE, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    }

    static void seedReviews() throws IOException {
        if (!Files.exists(REVIEWS_FILE))
            Files.writeString(REVIEWS_FILE, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    }

    static List<AppUser> loadUsers() throws IOException {
        List<AppUser> users = new ArrayList<>();
        if (!Files.exists(USERS_FILE))
            return users;
        for (String line : Files.readAllLines(USERS_FILE, StandardCharsets.UTF_8)) {
            if (line.isBlank())
                continue;
            AppUser user = AppUser.fromRecord(line);
            if (user != null)
                users.add(user);
        }

        return users;
    }

    static AppUser findUser(String username) throws IOException {
        for (AppUser user : loadUsers()) {
            if (user.getUsername().equalsIgnoreCase(safe(username).trim()))
                return user;
        }

        return null;
    }

    static AppUser authenticateUser(String username, String password) throws IOException {
        String login = safe(username).trim();
        AppUser user = findUser(login);
        if (user == null && login.contains("@")) {
            String normalizedLogin = normalizeEmailAddress(login);
            for (AppUser candidate : loadUsers()) {
                if (candidate.getEmail().equalsIgnoreCase(normalizedLogin)) {
                    user = candidate;
                    break;
                }
            }
        }
        if (user != null && user.isBanned()) {
            logActivity("LOGIN_BLOCKED_BANNED", user.getUsername() + " attempted to sign in while banned");
            return null;
        }
        if (user != null && user.checkPassword(password)) {
            logActivity("LOGIN_SUCCESS", user.getUsername() + " logged in");
            return user;
        }
        logActivity("LOGIN_FAILED", safe(username) + " failed login attempt");
        return null;
    }

    static String registerAdminUser(String username, String email, String phone, String password, String adminCode)
            throws IOException {
        username = safe(username).trim();

        email = normalizeEmailAddress(email);
        phone = safe(phone).trim();
        password = safe(password).trim();
        adminCode = safe(adminCode).trim();
        String requiredCode = System.getProperty("automart.admin.signup.code", "AUTO-MART-ADMIN");
        if (!requiredCode.equals(adminCode))
            return "Invalid admin invite code.";
        if (findUser(username) != null)
            return "That admin username is already taken.";
        String emailError = validateEmailAddress(email);
        if (emailError != null)
            return emailError;
        String phoneError = validatePhoneNumber(phone);
        if (phoneError != null)
            return phoneError;
        phone = digitsOnly(phone);
        if (!STRONG_PASSWORD_PATTERN.matcher(password).matches())
            return "Use at least 8 characters with upper, lower, and a number.";
        AdminUser user = new AdminUser(username, email, phone, password);
        Files.writeString(USERS_FILE, user.toRecord() + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        logActivity("ADMIN_REGISTERED", username + " created an admin account");

        return null;
    }

    static String registerUser(String username, String email, String phone, String password, String role)
            throws IOException {
        username = safe(username).trim();

        email = normalizeEmailAddress(email);
        phone = safe(phone).trim();
        password = safe(password).trim();
        if (findUser(username) != null)
            return "That username is already taken.";
        String emailError = validateEmailAddress(email);
        if (emailError != null)
            return emailError;
        String phoneError = validatePhoneNumber(phone);
        if (phoneError != null)
            return phoneError;
        phone = digitsOnly(phone);
        if (!STRONG_PASSWORD_PATTERN.matcher(password).matches())
            return "Use at least 8 characters with upper, lower, and a number.";
        AppUser user = switch (safe(role).toLowerCase(Locale.ROOT)) {
            case "seller" -> new SellerUser(username, email, phone, password);
            default -> new BuyerUser(username, email, phone, password);
        };
        Files.writeString(USERS_FILE, user.toRecord() + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        logActivity("USER_REGISTERED", username + " registered as " + user.getRole());

        return null;
    }

    static boolean updateUserStatus(String usernameOrEmail, String status, String banReason, String adminNote)
            throws IOException {
        String target = safe(usernameOrEmail).trim();
        if (target.isBlank())
            return false;
        List<AppUser> users = loadUsers();
        boolean updated = false;
        for (AppUser user : users) {
            if (user.getUsername().equalsIgnoreCase(target) || user.getEmail().equalsIgnoreCase(target)) {
                user.setStatus(status);
                user.setBanReason(banReason);
                user.setAdminNote(adminNote);
                updated = true;
                logActivity("USER_STATUS_UPDATED", user.getUsername() + " status set to " + user.getStatus());
                break;
            }
        }
        if (updated) {
            Files.write(USERS_FILE, users.stream().map(AppUser::toRecord).toList(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        return updated;
    }

    static String updateCurrentUser(HttpExchange exchange, Map<String, String> form) throws IOException {
        AppUser current = currentUser(exchange);
        if (current == null)
            return "No active user found.";
        List<AppUser> users = loadUsers();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equalsIgnoreCase(current.getUsername())) {
                String newUsername = safe(form.getOrDefault("username", current.getUsername())).trim();
                String newEmail = normalizeEmailAddress(form.getOrDefault("email", current.getEmail()));
                String newPhone = safe(form.getOrDefault("phone", current.getPhone())).trim();
                String newPassword = safe(form.getOrDefault("password", "")).trim();
                String emailError = validateEmailAddress(newEmail);
                if (emailError != null)
                    return emailError;
                String phoneError = validatePhoneNumber(newPhone);
                if (phoneError != null)
                    return phoneError;
                newPhone = digitsOnly(newPhone);
                users.get(i).setUsername(newUsername);
                users.get(i).setEmail(newEmail);
                users.get(i).setPhone(newPhone);
                if (!newPassword.isBlank()) {
                    if (!STRONG_PASSWORD_PATTERN.matcher(newPassword).matches())
                        return "Use at least 8 characters with upper, lower, and a number.";
                    users.get(i).setPassword(newPassword);
                }
                Files.write(USERS_FILE, users.stream().map(AppUser::toRecord).toList(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                startSession(exchange, users.get(i).getUsername());

                logActivity("USER_UPDATED", current.getUsername() + " updated profile details");
                return "Profile updated successfully.";
            }
        }
        return "Profile update failed.";
    }

    static void deleteCurrentUser(HttpExchange exchange) throws IOException {
        AppUser current = currentUser(exchange);
        if (current == null)
            return;
        List<String> lines = new ArrayList<>();
        for (AppUser user : loadUsers()) {
            if (!user.getUsername().equalsIgnoreCase(current.getUsername()))
                lines.add(user.toRecord());
        }

        Files.write(USERS_FILE, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        logActivity("USER_DELETED", current.getUsername() + " deleted their account");

    }

    static List<PurchaseRequest> loadRequests() throws IOException {
        List<PurchaseRequest> out = new ArrayList<>();
        if (!Files.exists(REQUESTS_FILE))
            return out;
        for (String line : Files.readAllLines(REQUESTS_FILE, StandardCharsets.UTF_8)) {
            if (line.isBlank())
                continue;
            PurchaseRequest request = PurchaseRequest.fromRecord(line);
            if (request != null)
                out.add(request);
        }

        return out;
    }

    static void saveAllRequests(List<PurchaseRequest> requests) throws IOException {
        Files.write(REQUESTS_FILE, requests.stream().map(PurchaseRequest::toRecord).toList(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    static String createRequest(HttpExchange exchange, Map<String, String> form) throws IOException {
        AppUser user = currentUser(exchange);
        Vehicle vehicle = findById(form.getOrDefault("vehicleId", ""));
        if (user == null || vehicle == null)
            return "Request failed.";
        String phone = safe(form.getOrDefault("contactPhone", user.getPhone())).trim();
        String phoneError = validatePhoneNumber(phone);
        if (phoneError != null)
            return phoneError;
        String note = safe(form.getOrDefault("note", ""));
        note = "Phone: " + digitsOnly(phone) + " | " + note;
        PurchaseRequest request = PurchaseRequest.create(vehicle.id, vehicle.title, user.getUsername(), note);
        Files.writeString(REQUESTS_FILE, request.toRecord() + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        logActivity("REQUEST_CREATED", user.getUsername() + " created request " + request.id + " for " + vehicle.title);
        return "Request submitted.";
    }

    static void handleRequestAction(HttpExchange exchange, Map<String, String> form) throws IOException {
        List<PurchaseRequest> requests = loadRequests();
        String id = safe(form.getOrDefault("id", ""));
        String action = safe(form.getOrDefault("action", ""));
        if ("delete".equals(action)) {
            requests.removeIf(r -> r.id.equals(id));
            saveAllRequests(requests);
            logActivity("REQUEST_DELETED", "Deleted request " + id);
            redirect(exchange, "/requests?msg=" + url("Request deleted."));
            return;
        }
        for (PurchaseRequest request : requests) {
            if (request.id.equals(id))
                request.status = safe(form.getOrDefault("status", request.status));
        }

        saveAllRequests(requests);
        logActivity("REQUEST_UPDATED", "Updated request " + id + " to " + safe(form.getOrDefault("status", "")));
        redirect(exchange, "/requests?msg=" + url("Request status updated."));
    }

    static List<ReviewEntry> loadReviews() throws IOException {
        List<ReviewEntry> out = new ArrayList<>();
        if (!Files.exists(REVIEWS_FILE))
            return out;
        for (String line : Files.readAllLines(REVIEWS_FILE, StandardCharsets.UTF_8)) {
            if (line.isBlank())
                continue;
            ReviewEntry review = ReviewEntry.fromRecord(line);
            if (review != null)
                out.add(review);
        }

        return out;
    }

    static void saveAllReviews(List<ReviewEntry> reviews) throws IOException {
        Files.write(REVIEWS_FILE, reviews.stream().map(ReviewEntry::toRecord).toList(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    static void createReview(HttpExchange exchange, Map<String, String> form) throws IOException {
        AppUser user = currentUser(exchange);
        Vehicle vehicle = findById(form.getOrDefault("vehicleId", ""));
        if (user == null || vehicle == null)
            return;
        ReviewEntry review = ReviewEntry.create(safe(form.getOrDefault("type", "PublicReview")),
                "REV-" + System.currentTimeMillis(), vehicle.id, vehicle.title, user.getUsername(),
                safe(form.getOrDefault("comment", "")), safe(form.getOrDefault("rating", "5")),
                LocalDateTime.now().format(STORED));
        Files.writeString(REVIEWS_FILE, review.toRecord() + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        logActivity("REVIEW_CREATED", user.getUsername() + " reviewed " + vehicle.title + " with rating " +
                review.rating);
    }

    static void handleReviewAction(HttpExchange exchange, Map<String, String> form) throws IOException {
        List<ReviewEntry> reviews = loadReviews();
        String id = safe(form.getOrDefault("id", ""));
        String action = safe(form.getOrDefault("action", ""));
        if ("delete".equals(action)) {
            reviews.removeIf(r -> r.id.equals(id));
            saveAllReviews(reviews);
            logActivity("REVIEW_DELETED", "Deleted review " + id);
            redirect(exchange, "/reviews?msg=" + url("Review deleted."));
            return;
        }
        for (ReviewEntry review : reviews) {
            if (review.id.equals(id))
                review.comment = safe(form.getOrDefault("comment", review.comment));
        }

        saveAllReviews(reviews);
        logActivity("REVIEW_UPDATED", "Updated review " + id);
        redirect(exchange, "/reviews?msg=" + url("Review updated."));
    }

    static boolean linearSearchModel(String title, String needle) {
        for (String token : safe(title).toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.contains(needle))
                return true;
        }

        return false;
    }

    static int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(safe(value).trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    static void logActivity(String event, String detail) {
        try {
            String line = LocalDateTime.now().format(STORED) + "	" + safe(event) + "	" + safe(detail);
            Files.writeString(LOGS_FILE, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }

    }

    static String hashPassword(String raw) {
        String clean = safe(raw).trim();
        if (clean.startsWith("salt$"))
            return clean;
        return "salt$" + Integer.toHexString(("AutoMartSalt::" + clean).hashCode());
    }

}
