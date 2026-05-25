package com.automart;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class AutoMartApplication {

    static final int PORT = 8080;

    // Project Paths
    static final Path ROOT = Paths.get("").toAbsolutePath();
    static final Path RESOURCES_DIR = ROOT.resolve("src/main/resources");
    static final Path STATIC_DIR = RESOURCES_DIR.resolve("static");
    static final Path TEMPLATES_DIR = RESOURCES_DIR.resolve("templates");
    static final Path DATA_DIR = RESOURCES_DIR.resolve("data");
    static final Path USERS_FILE = DATA_DIR.resolve("users.txt");

    public static void main(String[] args) throws Exception {

        bootstrap();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Main Router
        server.createContext("/", new RouteHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("🔥 AutoMart running at http://localhost:" + PORT);
    }

    // Create required folders/files
    private static void bootstrap() throws IOException {

        Files.createDirectories(DATA_DIR);

        if (!Files.exists(USERS_FILE)) {
            Files.writeString(USERS_FILE, "", StandardCharsets.UTF_8);
            System.out.println("📁 Created users.txt");
        }
    }

    /**
     * TEMPLATE RENDER ENGINE
     */
    static String renderPage(String templateSubPath) {

        try {

            Path targetTemplate = TEMPLATES_DIR.resolve(templateSubPath);

            Path baseLayout = TEMPLATES_DIR.resolve("layout/base.html");

            Path headerPath = TEMPLATES_DIR.resolve("partials/header.html");

            Path footerPath = TEMPLATES_DIR.resolve("partials/footer.html");

            // Check template exists
            if (!Files.exists(targetTemplate)) {

                return "<h1>404 Template Not Found</h1><p>Missing: "
                        + templateSubPath + "</p>";
            }

            // Read HTML files
            String content = Files.readString(targetTemplate, StandardCharsets.UTF_8);

            String layout = Files.exists(baseLayout)
                    ? Files.readString(baseLayout, StandardCharsets.UTF_8)
                    : "{{content_html}}";

            String header = Files.exists(headerPath)
                    ? Files.readString(headerPath, StandardCharsets.UTF_8)
                    : "";

            String footer = Files.exists(footerPath)
                    ? Files.readString(footerPath, StandardCharsets.UTF_8)
                    : "";

            // Replace placeholders
            layout = layout.replace("{{title}}", "Auto Mart");

            layout = layout.replace("{{body_class}}", "");

            layout = layout.replace("{{header_html}}", header);

            layout = layout.replace("{{content_html}}", content);

            layout = layout.replace("{{footer_html}}", footer);

            layout = layout.replace("{{extra_css}}", "");

            layout = layout.replace("{{extra_js}}", "");

            return layout;

        } catch (IOException e) {

            e.printStackTrace();

            return "<h1>500 Internal Server Error</h1><p>"
                    + e.getMessage() + "</p>";
        }
    }

    /**
     * ROUTER
     */
    private static class RouteHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String path = exchange.getRequestURI().getPath();

            // STATIC FILES
            if (path.startsWith("/assets/")) {

                Path staticFilePath = STATIC_DIR.resolve(
                        path.substring("/assets/".length())
                );

                if (Files.exists(staticFilePath)
                        && !Files.isDirectory(staticFilePath)) {

                    String contentType = "text/plain";

                    if (path.endsWith(".css")) {
                        contentType = "text/css";
                    } else if (path.endsWith(".js")) {
                        contentType = "application/javascript";
                    } else if (path.endsWith(".png")) {
                        contentType = "image/png";
                    } else if (path.endsWith(".jpg")
                            || path.endsWith(".jpeg")) {
                        contentType = "image/jpeg";
                    } else if (path.endsWith(".svg")) {
                        contentType = "image/svg+xml";
                    }

                    byte[] fileBytes = Files.readAllBytes(staticFilePath);

                    exchange.getResponseHeaders()
                            .set("Content-Type", contentType);

                    exchange.sendResponseHeaders(200, fileBytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(fileBytes);
                    }

                } else {

                    send404(exchange);
                }

                return;
            }

            // PAGE ROUTES
            String htmlResponse;

            switch (path) {

                case "/":
                case "/signin":
                    htmlResponse = renderPage("auth/signin.html");
                    break;

                case "/signup":
                    htmlResponse = renderPage("auth/signup.html");
                    break;

                case "/admin/login":
                    htmlResponse = renderPage("auth/admin-login.html");
                    break;

                case "/admin/signup":
                    htmlResponse = renderPage("auth/admin-signup.html");
                    break;

                case "/profile/buyer":
                    htmlResponse = renderPage("profile/buyer-profile.html");
                    break;

                case "/profile/seller":
                    htmlResponse = renderPage("profile/premium-profile.html");
                    break;

                default:
                    send404(exchange);
                    return;
            }

            // SEND HTML
            byte[] responseBytes =
                    htmlResponse.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                    .set("Content-Type", "text/html; charset=UTF-8");

            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }

        /**
         * 404 PAGE
         */
        private void send404(HttpExchange exchange) throws IOException {

            String errorMsg = "<h1>404 Route Not Found</h1>";

            byte[] errorBytes =
                    errorMsg.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                    .set("Content-Type", "text/html");

            exchange.sendResponseHeaders(404, errorBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }
    }
}