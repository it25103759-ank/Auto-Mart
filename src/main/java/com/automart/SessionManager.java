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

final class SessionManager {
    static void startSession(HttpExchange exchange, String username) {
        String safeName = safe(username).replace(";", "").trim();
        if (safeName.isBlank()) safeName = "guest-" + UUID.randomUUID().toString().substring(0, 8);
        exchange.getResponseHeaders().add("Set-Cookie", AUTH_COOKIE + "=" + url(safeName) + "; Path=/; HttpOnly; SameSite=Lax");
    }
    static void clearSession(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Set-Cookie", AUTH_COOKIE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
    }
    static String currentUsername(HttpExchange exchange) {
        return readCookie(exchange, AUTH_COOKIE);
    }
}
