package com.automart;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.automart.AutoMartApplication.*;

final class TemplateRenderer {
    private TemplateRenderer() {
    }

    static String renderLayout(HttpExchange exchange,
                               String title,
                               String contentHtml,
                               boolean showChrome,
                               String bodyClass,
                               String extraCss,
                               String extraJs) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("title", esc(title));
        vars.put("body_class", safe(bodyClass));
        vars.put("extra_css", safe(extraCss));
        vars.put("extra_js", safe(extraJs));
        vars.put("header_html", showChrome ? header(exchange) : "");
        vars.put("content_html", safe(contentHtml));
        vars.put("footer_html", showChrome ? footer() : "");
        return render("layout/base.html", vars);
    }

    static String renderAuthPage(String error, boolean signUp, Map<String, String> values) {
        Map<String, String> vars = new LinkedHashMap<>();
        String role = normalizeRole(values.getOrDefault("role", "buyer"));
        vars.put("error_html", alert(error));
        vars.put("username_value", esc(values.getOrDefault("username", "")));
        vars.put("email_value", esc(values.getOrDefault("email", "")));
        vars.put("phone_value", esc(values.getOrDefault("phone", "")));
        vars.put("full_name_value", esc(values.getOrDefault("fullName", "")));
        vars.put("business_name_value", esc(values.getOrDefault("businessName", "")));
        vars.put("shop_location_value", esc(values.getOrDefault("shopLocation", "")));
        vars.put("seller_nic_value", esc(values.getOrDefault("sellerNic", "")));
        vars.put("role_value", role);
        vars.put("role_label", esc(capitalize(role)));
        vars.put("buyer_role_active", "buyer".equals(role) ? "is-active" : "");
        vars.put("seller_role_active", "seller".equals(role) ? "is-active" : "");

        String content = render(signUp ? "auth/signup.html" : "auth/signin.html", vars);
        return renderLayout(null,
                signUp ? "Sign Up" : "Sign In",
                content,
                false,
                "auth-premium-body",
                "<link rel=\"stylesheet\" href=\"/assets/css/auth-premium.css\">",
                "<script src=\"/assets/js/auth-premium.js\"></script>");
    }

    static String renderAdminLoginPage(String error, String username) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("error_html", alert(error));
        vars.put("username_value", "");
        String content = render("auth/admin-login.html", vars);
        return renderLayout(null,
                "Admin Login",
                content,
                false,
                "auth-premium-body",
                "<link rel=\"stylesheet\" href=\"/assets/css/auth-premium.css\">",
                "<script src=\"/assets/js/auth-premium.js\"></script>");
    }


    static String renderAdminSignupPage(String error, Map<String, String> values) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("error_html", alert(error));
        vars.put("admin_code_value", esc(values.getOrDefault("adminCode", "")));
        vars.put("full_name_value", esc(values.getOrDefault("fullName", "")));
        vars.put("username_value", esc(values.getOrDefault("username", "")));
        vars.put("email_value", esc(values.getOrDefault("email", "")));
        vars.put("phone_value", esc(values.getOrDefault("phone", "")));
        String content = render("auth/admin-signup.html", vars);
        return renderLayout(null,
                "Admin Sign Up",
                content,
                false,
                "auth-premium-body",
                "<link rel=\"stylesheet\" href=\"/assets/css/auth-premium.css\">",
                "<script src=\"/assets/js/auth-premium.js\"></script>");
    }

    static String renderHeader(HttpExchange exchange) {
        AppUser user = currentUser(exchange);
        boolean admin = user != null && user.isAdmin();
        String pathName = exchange == null || exchange.getRequestURI() == null ? "/" : exchange.getRequestURI().getPath();
        String initials = user == null ? "AM" : initials(user.getUsername());

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("home_active", "/".equals(pathName) ? "is-active" : "");
        vars.put("inventory_active", (pathName.startsWith("/inventory") || pathName.startsWith("/vehicle")) ? "is-active" : "");
        vars.put("wishlist_active", pathName.startsWith("/wishlist") ? "is-active" : "");
        vars.put("sell_active", pathName.startsWith("/sell") ? "is-active" : "");
        vars.put("requests_active", pathName.startsWith("/requests") ? "is-active" : "");
        vars.put("reviews_active", pathName.startsWith("/reviews") ? "is-active" : "");
        vars.put("user_initials", esc(initials));
        vars.put("user_name", esc(user == null ? "Account" : user.getUsername()));
        vars.put("user_role", esc(user == null ? "Guest" : user.getRole()));
        String roleLower = user == null ? "guest" : user.getRole().toLowerCase();
        vars.put("role_class", esc("role-" + roleLower));
        vars.put("sell_nav_html", user != null && (user.isAdmin() || "seller".equalsIgnoreCase(user.getRole())) ? "<a class='top-nav-link " + ("sell".equals(pathName) || pathName.startsWith("/sell") ? "is-active" : "") + "' href='/sell'><span>Sell Car</span></a>" : "");
        vars.put("profile_nav_html", user != null && "buyer".equalsIgnoreCase(user.getRole())
                ? "<a class='top-nav-link " + (pathName.startsWith("/buyer-profile") ? "is-active" : "") + "' href='/buyer-profile'><span>Buyer Profile</span></a>"
                : user != null && user.isAdmin()
                    ? "<a class='top-nav-link " + (pathName.startsWith("/admin") ? "is-active" : "") + "' href='/admin'><span>Admin Panel</span></a>"
                    : "<a class='top-nav-link " + (pathName.startsWith("/profile") ? "is-active" : "") + "' href='/profile'><span>Seller Profile</span></a>");
        vars.put("profile_menu_links", user != null && "buyer".equalsIgnoreCase(user.getRole())
                ? "<a href='/buyer-profile'>Buyer Profile</a><a href='/wishlist'>Wishlist</a><a href='/settings'>Settings</a>"
                : user != null && user.isAdmin()
                    ? "<a href='/admin'>Admin Panel</a><a href='/profile'>Admin Profile</a><a href='/settings'>Settings</a>"
                    : "<a href='/profile'>Seller Profile</a><a href='/sell'>Sell Car</a><a href='/settings'>Settings</a>");
        vars.put("admin_link", admin ? "<a href='/admin'>Admin Panel</a>" : "");
        return render("partials/header.html", vars);
    }

    static String renderFooter() {
        return render("partials/footer.html", Map.of());
    }

    private static String render(String relativePath, Map<String, String> vars) {
        Path file = TEMPLATES_DIR.resolve(relativePath).normalize();
        if (!file.startsWith(TEMPLATES_DIR)) {
            throw new IllegalArgumentException("Invalid template path: " + relativePath);
        }

        try {
            String template = Files.readString(file, StandardCharsets.UTF_8);
            String html = template;
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                html = html.replace("{{" + entry.getKey() + "}}", safe(entry.getValue()));
            }
            return html.replaceAll("\\{\\{[a-zA-Z0-9_-]+}}", "");
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read template: " + relativePath, ex);
        }
    }

    private static String alert(String error) {
        if (error == null || error.isBlank()) {
            return "";
        }
        return "<div class='authx-alert'>" + esc(error) + "</div>";
    }

    private static String normalizeRole(String role) {
        return "seller".equalsIgnoreCase(role) ? "seller" : "buyer";
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Buyer";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }
}
